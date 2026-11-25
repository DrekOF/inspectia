/*
 * Nombre: GuardarAnalisis.java
 * Descripción: Caso de uso que guarda, en una sola transacción, el análisis de una muestra tomada de la cola: la
 *              revisión de sus fotos, sus granos, sus conteos, su modelo y el resultado del lote con los rangos vigentes
 *              de la empresa; o la devuelve a pendiente sin resultado, con su motivo o con la foto que hay que repetir;
 *              con la muestra 2, recalcula el lote con la suma de las dos y descarta el reporte PDF de la 1 (RF-06,
 *              RF-07, RF-09, RF-10, RF-12, RF-13 C2, RF-15 C5, RNF-02 C3, D-25, D-44, D-46).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import co.edu.usco.inspectia.analisis.dominio.ConteoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeDevolucion;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.ParametrosDelAnalisis;
import co.edu.usco.inspectia.analisis.dominio.ReglasDeConfianza;
import co.edu.usco.inspectia.analisis.dominio.ResultadoDelLote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda el análisis de una muestra que la cola tomó y ya se clasificó (RF-10, RF-12). Es la segunda transacción del
 * análisis: la clasificación pasa antes y fuera de toda transacción ({@link AtenderCola}).
 *
 * <p>Bloquea el lote primero, como {@link SubirFotos#confirmar} y {@link EnviarMuestra}. Después decide, en este orden
 * (specs: contrato REST del servicio de IA, sección 3), y cambia la muestra solo si sigue «en proceso» con su hora de
 * inicio: si otra ejecución ya la tomó, no guarda nada.
 *
 * <ol>
 *   <li><b>La revisión de las fotos</b> (RF-06, RF-07, D-46): con el servicio de IA, cada foto trae su revisión. Si
 *       alguna queda por repetir, la muestra vuelve a pendiente sin motivo propio (el motivo va en la foto, D-44) y se
 *       guarda la revisión de todas. No cuenta como repetición (RF-09, restricción 6).</li>
 *   <li><b>El mínimo de 100 granos</b> (RF-07 C3, D-23): con menos, vuelve con {@code POCOS_GRANOS}, con la revisión de
 *       las fotos guardada y sin granos ni resultado.</li>
 *   <li><b>Los granos dudosos</b> (RF-09): si pasan del máximo y la muestra no ha gastado sus 2 repeticiones, la foto
 *       con más dudosos (en empate, la de número menor) queda por repetir con {@code GRANOS_DUDOSOS}, sus granos se
 *       guardan para mostrar cuáles son, y la muestra vuelve con una repetición más y la copia del umbral y del máximo.
 *       Una sola foto por vez. Corre también con el clasificador falso: es una regla del dominio.</li>
 *   <li><b>Terminar</b>: guarda sus granos en lotes de inserción (antes borra los que hubiera en sus fotos), aprueba sus
 *       fotos y calcula el resultado del lote con sus muestras terminadas, los recall del modelo de cada una (D-39) y
 *       los rangos vigentes de la empresa (RF-13 C2). Con 2 repeticiones y más del máximo, sale con el aviso de
 *       confianza baja (RF-09 C4). El lote queda analizado.</li>
 * </ol>
 *
 * <p>La muestra 2 (D-25) llega con el lote ya analizado con la 1: se guarda igual, aprueba solo sus fotos, y el
 * resultado se recalcula con la suma de las dos y los rangos de ese momento, sin sugerencia ni límite (D-47). El lote
 * olvida la ruta de su reporte, que se rehace con el resultado nuevo, y los archivos del reporte de la 1 se borran
 * después de confirmar la transacción (RF-15 C5, {@link ReporteDescartado}). Todo se confirma junto o nada.
 */
@Slf4j
@Service
public class GuardarAnalisis {

    private final RepositorioDeLotes lotes;
    private final RepositorioDeMuestras muestras;
    private final RepositorioDeModelos modelos;
    private final RangosDeLaEmpresa rangos;
    private final ParametrosDelAnalisis parametros;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    /** Cómo terminó el intento de guardar una muestra. */
    public enum Desenlace {

        /** La muestra terminó y el lote quedó analizado. */
        TERMINADA,

        /** Una foto no pasó la revisión de la cola: la muestra volvió a pendiente, sin resultado (RF-06, RF-07). */
        DEVUELTA_POR_FOTOS_POR_REPETIR,

        /** La muestra tenía menos de 100 granos: volvió a pendiente, sin resultado (RF-07 C3). */
        DEVUELTA_POR_POCOS_GRANOS,

        /** Tenía más dudosos que el máximo: volvió a pendiente para repetir la foto con más dudosos (RF-09 C1). */
        DEVUELTA_POR_DUDOSOS,

        /** La muestra ya no estaba «en proceso» con esa hora de inicio: otra ejecución la tomó. No se guardó nada. */
        YA_NO_ESTABA_EN_PROCESO
    }

    /**
     * Crea el caso de uso; Spring le pasa sus dependencias.
     *
     * @param lotes los lotes guardados
     * @param muestras las muestras, sus fotos y sus granos
     * @param modelos los modelos de IA registrados
     * @param rangos los rangos vigentes de cada empresa
     * @param parametros los parámetros del análisis
     * @param eventos para borrar los archivos del reporte descartado (RF-15 C5)
     * @param reloj el reloj de la aplicación, en UTC
     */
    public GuardarAnalisis(RepositorioDeLotes lotes, RepositorioDeMuestras muestras, RepositorioDeModelos modelos,
            RangosDeLaEmpresa rangos, ParametrosDelAnalisis parametros, ApplicationEventPublisher eventos,
            Clock reloj) {
        this.lotes = lotes;
        this.muestras = muestras;
        this.modelos = modelos;
        this.rangos = rangos;
        this.parametros = parametros;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /**
     * Guarda el análisis de una muestra, en una transacción.
     *
     * @param muestra la muestra tomada de la cola
     * @param clasificacion lo que respondió el clasificador
     * @return cómo terminó
     * @throws IllegalStateException si el lote no existe, si el modelo no está registrado o si un grano o una revisión
     *     no es de una foto de la muestra: la transacción se deshace y la cola devuelve la muestra a pendiente
     * @throws IllegalArgumentException si un grano tiene una posición o una confianza que no sirven
     */
    @Transactional
    public Desenlace guardar(MuestraTomada muestra, Clasificacion clasificacion) {
        UUID empresa = muestra.empresaId();
        lotes.estadoParaActualizar(empresa, muestra.loteId())
                .orElseThrow(() -> new IllegalStateException("El lote " + muestra.loteId() + " no existe"));
        Map<Integer, UUID> fotoPorNumero = muestras.fotos(empresa, muestra.id()).stream()
                .collect(Collectors.toMap(FotoGuardada::numero, FotoGuardada::id));
        Map<Integer, RevisionDeFoto> revisiones = clasificacion.revisiones();
        revisiones.keySet().forEach(numero -> fotoDe(numero, fotoPorNumero));

        if (revisiones.values().stream().anyMatch(RevisionDeFoto::pideRepetir)) {
            log.info("La muestra {} tiene fotos por repetir: vuelve a pendiente sin resultado", muestra.id());
            return devolverConRevisiones(muestra, null, revisiones, fotoPorNumero,
                    Desenlace.DEVUELTA_POR_FOTOS_POR_REPETIR);
        }
        List<GranoClasificado> granos = clasificacion.granos();
        if (!ConteoDeMuestra.alcanzaElMinimo(granos.size())) {
            log.warn("La muestra {} tiene {} granos, menos de {}: vuelve a pendiente sin resultado", muestra.id(),
                    granos.size(), ConteoDeMuestra.MINIMO_DE_GRANOS);
            return devolverConRevisiones(muestra, MotivoDeDevolucion.POCOS_GRANOS, revisiones, fotoPorNumero,
                    Desenlace.DEVUELTA_POR_POCOS_GRANOS);
        }
        ModeloUsado usado = clasificacion.modelo();
        ModeloRegistrado modelo = modelos.buscar(usado.nombre(), usado.version())
                .orElseThrow(() -> new IllegalStateException("El modelo " + usado.nombre() + " " + usado.version()
                        + " no está registrado en la tabla modelo"));
        List<GranoDeFoto> aGuardar = granos.stream().map(grano -> deLaFoto(grano, fotoPorNumero)).toList();
        int sanos = (int) aGuardar.stream().filter(grano -> grano.clase() == ClaseDeGrano.SANO).count();
        ReglasDeConfianza confianza = parametros.reglasDeConfianza();
        int dudosos = (int) aGuardar.stream().filter(grano -> confianza.esDudoso(grano.confianza())).count();

        if (confianza.pideRepetir(dudosos, aGuardar.size(), muestra.repeticiones())) {
            return pedirRepeticion(muestra, revisiones, fotoPorNumero, aGuardar, confianza);
        }
        Instant fin = reloj.instant().truncatedTo(ChronoUnit.MICROS);
        TerminoDeMuestra termino = new TerminoDeMuestra(modelo.id(), aGuardar.size(), sanos,
                confianza.aviso(dudosos, aGuardar.size(), muestra.repeticiones()),
                parametros.umbralConfianzaBaja(), parametros.porcentajeMaxDudosos(), fin);
        if (!muestras.terminar(muestra, termino)) {
            log.warn("La muestra {} ya no estaba en proceso desde {}: no se guarda", muestra.id(), muestra.fechaInicio());
            return Desenlace.YA_NO_ESTABA_EN_PROCESO;
        }
        muestras.borrarGranos(empresa, fotoPorNumero.values());
        muestras.guardarGranos(empresa, aGuardar);
        muestras.aprobarFotos(empresa, muestra.id());
        if (!revisiones.isEmpty()) {
            // Con el servicio, cada foto queda con los granos que la cola clasificó en ella.
            Map<UUID, Long> porFoto = granosPorFoto(aGuardar);
            fotoPorNumero.values().forEach(foto -> muestras.guardarRevision(empresa, foto,
                    RevisionDeFoto.aprobada(Math.toIntExact(porFoto.getOrDefault(foto, 0L)))));
        }
        ResultadoDelLote resultado = ResultadoDelLote.calcular(muestras.conteosTerminados(empresa, muestra.loteId()),
                rangos.vigentes(empresa), parametros);
        String reporteAnterior = lotes.datosDelReporte(empresa, muestra.loteId())
                .map(DatosDelReporte::rutaDelReporte).orElse(null);
        lotes.guardarResultado(empresa, muestra.loteId(), resultado, fin);
        if (reporteAnterior != null) {
            eventos.publishEvent(new ReporteDescartado(reporteAnterior));
        }
        return Desenlace.TERMINADA;
    }

    /**
     * Devuelve a pendiente, sin resultado, una muestra cuyo análisis falló, en una transacción aparte (RF-08). Bloquea
     * el lote primero, como al guardar.
     *
     * @param muestra la muestra tomada de la cola
     * @return {@code true} si la devolvió; {@code false} si ya no estaba «en proceso» con esa hora de inicio
     */
    @Transactional
    public boolean devolver(MuestraTomada muestra) {
        lotes.estadoParaActualizar(muestra.empresaId(), muestra.loteId());
        return muestras.devolver(muestra);
    }

    /**
     * Devuelve a pendiente, sin resultado y con su motivo, una muestra cuyo análisis falló, en una transacción aparte
     * (RF-08 C6, D-44). Bloquea el lote primero, como al guardar.
     *
     * @param muestra la muestra tomada de la cola
     * @param motivo por qué vuelve, como {@code SIN_RESPUESTA_DE_LA_IA}
     * @return {@code true} si la devolvió; {@code false} si ya no estaba «en proceso» con esa hora de inicio
     */
    @Transactional
    public boolean devolver(MuestraTomada muestra, MotivoDeDevolucion motivo) {
        lotes.estadoParaActualizar(muestra.empresaId(), muestra.loteId());
        return muestras.devolver(muestra, Objects.requireNonNull(motivo, "motivo"));
    }

    /** Devuelve la muestra con su motivo y, si sigue en proceso, guarda la revisión de cada foto (D-44, D-46). */
    private Desenlace devolverConRevisiones(MuestraTomada muestra, MotivoDeDevolucion motivo,
            Map<Integer, RevisionDeFoto> revisiones, Map<Integer, UUID> fotoPorNumero, Desenlace desenlace) {
        if (!muestras.devolver(muestra, motivo)) {
            return Desenlace.YA_NO_ESTABA_EN_PROCESO;
        }
        revisiones.forEach((numero, revision) ->
                muestras.guardarRevision(muestra.empresaId(), fotoDe(numero, fotoPorNumero), revision));
        return desenlace;
    }

    /**
     * Pide repetir la foto con más dudosos (RF-09 C1): la muestra vuelve con una repetición más y la copia de los
     * parámetros, las demás fotos guardan su revisión (si la hay), la foto queda por repetir con sus granos clasificados
     * como conteo y sus granos se guardan, así la página muestra cuáles son los dudosos.
     */
    private Desenlace pedirRepeticion(MuestraTomada muestra, Map<Integer, RevisionDeFoto> revisiones,
            Map<Integer, UUID> fotoPorNumero, List<GranoDeFoto> granos, ReglasDeConfianza confianza) {
        UUID empresa = muestra.empresaId();
        int numero = conMasDudosos(granos, fotoPorNumero, confianza);
        UUID foto = fotoPorNumero.get(numero);
        if (!muestras.pedirRepeticion(muestra, parametros.umbralConfianzaBaja(), parametros.porcentajeMaxDudosos())) {
            return Desenlace.YA_NO_ESTABA_EN_PROCESO;
        }
        log.info("La muestra {} tiene más del {} % de granos dudosos: pide repetir la foto {} (repetición {} de {})",
                muestra.id(), parametros.porcentajeMaxDudosos(), numero, muestra.repeticiones() + 1,
                ReglasDeConfianza.MAXIMO_DE_REPETICIONES);
        revisiones.forEach((otra, revision) -> {
            if (otra != numero) {
                muestras.guardarRevision(empresa, fotoDe(otra, fotoPorNumero), revision);
            }
        });
        List<GranoDeFoto> deLaFoto = granos.stream().filter(grano -> grano.fotoId().equals(foto)).toList();
        muestras.guardarRevision(empresa, foto, RevisionDeFoto.porRepetir(MotivoDeFoto.GRANOS_DUDOSOS,
                deLaFoto.size()));
        muestras.borrarGranos(empresa, List.of(foto));
        muestras.guardarGranos(empresa, deLaFoto);
        return Desenlace.DEVUELTA_POR_DUDOSOS;
    }

    /** El número de la foto con más granos dudosos; en empate, el menor (RF-09 C1). */
    private static int conMasDudosos(List<GranoDeFoto> granos, Map<Integer, UUID> fotoPorNumero,
            ReglasDeConfianza confianza) {
        Map<UUID, Integer> numeroDe = new HashMap<>();
        fotoPorNumero.forEach((numero, foto) -> numeroDe.put(foto, numero));
        Map<Integer, Long> dudososPorFoto = granos.stream()
                .filter(grano -> confianza.esDudoso(grano.confianza()))
                .collect(Collectors.groupingBy(grano -> numeroDe.get(grano.fotoId()), Collectors.counting()));
        Optional<Map.Entry<Integer, Long>> mayor = dudososPorFoto.entrySet().stream()
                .min(Comparator.<Map.Entry<Integer, Long>>comparingLong(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey));
        return mayor.orElseThrow(() -> new IllegalStateException("Una muestra que pasa del máximo tiene dudosos"))
                .getKey();
    }

    private static Map<UUID, Long> granosPorFoto(List<GranoDeFoto> granos) {
        return granos.stream().collect(Collectors.groupingBy(GranoDeFoto::fotoId, Collectors.counting()));
    }

    private static UUID fotoDe(int numero, Map<Integer, UUID> fotoPorNumero) {
        UUID foto = fotoPorNumero.get(numero);
        if (foto == null) {
            throw new IllegalStateException("El clasificador habla de la foto " + numero + ", que la muestra no tiene");
        }
        return foto;
    }

    /**
     * El grano con el identificador de su foto y la confianza con 2 decimales, como la guarda la base: un grano se
     * compara con el umbral con el valor guardado, así la pantalla y las estadísticas coinciden.
     */
    private static GranoDeFoto deLaFoto(GranoClasificado grano, Map<Integer, UUID> fotoPorNumero) {
        UUID foto = fotoPorNumero.get(grano.numeroDeFoto());
        if (foto == null) {
            throw new IllegalStateException("Un grano dice estar en la foto " + grano.numeroDeFoto()
                    + ", que la muestra no tiene");
        }
        Objects.requireNonNull(grano.clase(), "clase");
        BigDecimal confianza = Objects.requireNonNull(grano.confianza(), "confianza").setScale(2, RoundingMode.HALF_UP);
        if (grano.x() < 0 || grano.y() < 0 || grano.ancho() < 1 || grano.alto() < 1) {
            throw new IllegalArgumentException("Un grano de la foto " + grano.numeroDeFoto() + " tiene una posición "
                    + "que no sirve: " + grano);
        }
        if (confianza.signum() < 0 || confianza.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("La confianza de un grano debe ir de 0 a 100, y es " + confianza);
        }
        return new GranoDeFoto(foto, grano.x(), grano.y(), grano.ancho(), grano.alto(), grano.clase(), confianza);
    }
}
