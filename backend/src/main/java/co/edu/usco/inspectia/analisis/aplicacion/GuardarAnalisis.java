/*
 * Nombre: GuardarAnalisis.java
 * Descripción: Caso de uso que guarda, en una sola transacción, el análisis de una muestra tomada de la cola: sus
 *              granos, sus conteos, su modelo y el resultado del lote con los rangos vigentes de la empresa; o la
 *              devuelve a pendiente sin resultado; con la muestra 2, recalcula el lote con la suma de las dos y descarta
 *              el reporte PDF de la 1 (RF-10, RF-12, RF-13 C2, RF-15 C5, RNF-02 C3, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import co.edu.usco.inspectia.analisis.dominio.ConteoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.ParametrosDelAnalisis;
import co.edu.usco.inspectia.analisis.dominio.ReglasDeConfianza;
import co.edu.usco.inspectia.analisis.dominio.ResultadoDelLote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * <p>Bloquea el lote primero, como {@link SubirFotos#confirmar} y {@link EnviarMuestra}. Después termina la muestra solo
 * si sigue «en proceso» con su hora de inicio: si otra ejecución ya la tomó, no guarda nada. Con la muestra terminada,
 * guarda sus granos en lotes de inserción, aprueba sus fotos y calcula el resultado del lote con sus muestras
 * terminadas, los recall del modelo de cada una (D-39) y los rangos vigentes de la empresa (RF-13 C2). El lote queda
 * analizado. Todo se confirma junto o nada.
 *
 * <p>La muestra 2 (D-25) llega con el lote ya analizado con la 1: se guarda igual, aprueba solo sus fotos, y el
 * resultado se recalcula con la suma de las dos y los rangos de ese momento, sin sugerencia ni límite (D-47). El lote
 * olvida la ruta de su reporte, que se rehace con el resultado nuevo, y los archivos del reporte de la 1 se borran
 * después de confirmar la transacción (RF-15 C5, {@link ReporteDescartado}).
 *
 * <p>Con menos de 100 granos no calcula el resultado: devuelve la muestra a pendiente (RF-07 C3, D-26).
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

        /** La muestra tenía menos de 100 granos: volvió a pendiente, sin resultado (RF-07 C3). */
        DEVUELTA_POR_POCOS_GRANOS,

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
     * @throws IllegalStateException si el lote no existe, si el modelo no está registrado o si un grano no es de una
     *     foto de la muestra: la transacción se deshace y la cola devuelve la muestra a pendiente
     * @throws IllegalArgumentException si un grano tiene una posición o una confianza que no sirven
     */
    @Transactional
    public Desenlace guardar(MuestraTomada muestra, Clasificacion clasificacion) {
        UUID empresa = muestra.empresaId();
        lotes.estadoParaActualizar(empresa, muestra.loteId())
                .orElseThrow(() -> new IllegalStateException("El lote " + muestra.loteId() + " no existe"));
        List<GranoClasificado> granos = clasificacion.granos();
        if (!ConteoDeMuestra.alcanzaElMinimo(granos.size())) {
            log.warn("La muestra {} tiene {} granos, menos de {}: vuelve a pendiente sin resultado", muestra.id(),
                    granos.size(), ConteoDeMuestra.MINIMO_DE_GRANOS);
            return muestras.devolver(muestra) ? Desenlace.DEVUELTA_POR_POCOS_GRANOS
                    : Desenlace.YA_NO_ESTABA_EN_PROCESO;
        }
        ModeloUsado usado = clasificacion.modelo();
        ModeloRegistrado modelo = modelos.buscar(usado.nombre(), usado.version())
                .orElseThrow(() -> new IllegalStateException("El modelo " + usado.nombre() + " " + usado.version()
                        + " no está registrado en la tabla modelo"));
        Map<Integer, UUID> fotoPorNumero = muestras.fotos(empresa, muestra.id()).stream()
                .collect(Collectors.toMap(FotoGuardada::numero, FotoGuardada::id));
        List<GranoDeFoto> aGuardar = granos.stream().map(grano -> deLaFoto(grano, fotoPorNumero)).toList();
        int sanos = (int) aGuardar.stream().filter(grano -> grano.clase() == ClaseDeGrano.SANO).count();
        ReglasDeConfianza confianza = parametros.reglasDeConfianza();
        int dudosos = (int) aGuardar.stream().filter(grano -> confianza.esDudoso(grano.confianza())).count();
        Instant fin = reloj.instant().truncatedTo(ChronoUnit.MICROS);
        TerminoDeMuestra termino = new TerminoDeMuestra(modelo.id(), aGuardar.size(), sanos,
                confianza.aviso(dudosos, aGuardar.size(), muestra.repeticiones()),
                parametros.umbralConfianzaBaja(), parametros.porcentajeMaxDudosos(), fin);
        if (!muestras.terminar(muestra, termino)) {
            log.warn("La muestra {} ya no estaba en proceso desde {}: no se guarda", muestra.id(), muestra.fechaInicio());
            return Desenlace.YA_NO_ESTABA_EN_PROCESO;
        }
        muestras.guardarGranos(empresa, aGuardar);
        muestras.aprobarFotos(empresa, muestra.id());
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
