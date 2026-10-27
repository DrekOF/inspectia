/*
 * Nombre: RangoRecomendacionServicio.java
 * Descripción: Operaciones de rango_recomendacion, que se edita en bloque: validan, guardan y responden
 *              (RF-13).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.9)
 * Versión: 1.9
 * Entidades: rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.rango;

import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operaciones de {@code rango_recomendacion} (RF-13, RF-02, RF-12, D-10).
 *
 * <p>Se edita en bloque: cada empresa tiene una fila por cada categoría, y las filas se editan todas
 * juntas. Al editar revisa, en este orden:
 * <ol>
 *   <li>Los datos de cada fila, con las validaciones del JSON (las revisa Spring antes de llamar al
 *       servicio).</li>
 *   <li>Que llegue una fila por cada categoría, sin repetir.</li>
 *   <li>Que no cambien los datos de solo lectura ({@code solo_lectura_si} del JSON).</li>
 *   <li>Las reglas que se programan a mano, con todas las filas juntas.</li>
 * </ol>
 * Si algo no se cumple responde 400 y no guarda ninguna fila: lo de cada operación va en una sola
 * transacción.
 *
 * <p>Cada operación recibe el usuario de la sesión y solo ve y modifica las filas de su empresa (RNF-03).
 * Editar y restaurar bloquean esas filas hasta terminar: si dos personas las cambian a la vez, la segunda
 * espera a que la primera guarde y trabaja sobre lo que quedó. Si a la empresa le falta una fila, editar
 * y restaurar la crean con su valor por defecto: así siempre tiene una por cada categoría.
 */
@Service
@Transactional(readOnly = true)
public class RangoRecomendacionServicio {

    private final RangoRecomendacionRepositorio repositorio;
    private final ReglasRangoRecomendacion reglas;

    /**
     * Crea el servicio; Spring le pasa sus dependencias.
     *
     * @param repositorio el repositorio de {@code rango_recomendacion}
     * @param reglas las reglas que se programan a mano
     */
    public RangoRecomendacionServicio(
            RangoRecomendacionRepositorio repositorio,
            ReglasRangoRecomendacion reglas) {
        this.repositorio = repositorio;
        this.reglas = reglas;
    }

    /**
     * Las filas de la empresa de la sesión, una por cada categoría, en el orden de la lista.
     *
     * @param sesion el usuario de la sesión: solo se listan las de su empresa
     * @return las filas
     */
    public List<RangoRecomendacionRespuesta> listar(UsuarioEnSesion sesion) {
        return repositorio.findAllByEmpresaId(sesion.empresaId()).stream()
                .sorted(Comparator.comparing(RangoRecomendacion::getCategoria))
                .map(RangoRecomendacionMapeador::respuesta)
                .toList();
    }

    /**
     * Edita todas las filas de la empresa a la vez.
     * Los cambios se revisan en copias y solo se aplican si todo está bien.
     *
     * @param solicitud las filas, ya validadas con el JSON
     * @param sesion el usuario de la sesión
     * @return las filas guardadas, en el orden de la lista
     * @throws ReglaIncumplidaExcepcion si no llega una fila por cada categoría, si cambia un
     *         dato de solo lectura o si no se cumple una regla (400)
     */
    @Transactional
    public List<RangoRecomendacionRespuesta> editar(RangoRecomendacionEdicionSolicitud solicitud,
            UsuarioEnSesion sesion) {
        revisarFilasCompletas(solicitud);
        Map<RangoRecomendacionCategoria, RangoRecomendacion> filas = completas(sesion);
        revisarSoloLectura(solicitud, filas);
        List<RangoRecomendacion> propuesta = new ArrayList<>();
        for (RangoRecomendacionFilaSolicitud fila : solicitud.filas()) {
            RangoRecomendacion copia = RangoRecomendacionMapeador.copia(filas.get(fila.categoria()));
            RangoRecomendacionMapeador.aplicar(fila, copia);
            propuesta.add(copia);
        }
        propuesta.sort(Comparator.comparing(RangoRecomendacion::getCategoria));
        revisarReglasAlEditar(propuesta, sesion);
        for (RangoRecomendacionFilaSolicitud fila : solicitud.filas()) {
            RangoRecomendacionMapeador.aplicar(fila, filas.get(fila.categoria()));
        }
        repositorio.flush();
        return respuestas(filas);
    }

    /**
     * Vuelve a los valores por defecto: los de {@link RangoRecomendacionValoresIniciales}, los mismos que recibe
     * cada empresa nueva.
     *
     * @param sesion el usuario de la sesión
     * @return las filas guardadas, en el orden de la lista
     */
    @Transactional
    public List<RangoRecomendacionRespuesta> restaurarPorDefecto(UsuarioEnSesion sesion) {
        Map<RangoRecomendacionCategoria, RangoRecomendacion> filas = completas(sesion);
        for (RangoRecomendacion defecto : RangoRecomendacionValoresIniciales.paraEmpresa(sesion.empresaId())) {
            RangoRecomendacion fila = filas.get(defecto.getCategoria());
            fila.setLimiteInferior(defecto.getLimiteInferior());
            fila.setRecomendacion(defecto.getRecomendacion());
        }
        repositorio.flush();
        return respuestas(filas);
    }

    /**
     * Las filas de la empresa, bloqueadas hasta que termine la transacción, una por cada categoría: si
     * falta alguna, la crea con su valor por defecto.
     */
    private Map<RangoRecomendacionCategoria, RangoRecomendacion> completas(UsuarioEnSesion sesion) {
        Map<RangoRecomendacionCategoria, RangoRecomendacion> filas = new EnumMap<>(RangoRecomendacionCategoria.class);
        for (RangoRecomendacion fila : repositorio.bloquearLasDeLaEmpresa(sesion.empresaId())) {
            filas.put(fila.getCategoria(), fila);
        }
        for (RangoRecomendacion defecto : RangoRecomendacionValoresIniciales.paraEmpresa(sesion.empresaId())) {
            if (!filas.containsKey(defecto.getCategoria())) {
                filas.put(defecto.getCategoria(), repositorio.save(defecto));
            }
        }
        return filas;
    }

    /** Llega una fila por cada categoría, sin repetir: si no, responde 400 en {@code filas}. */
    private static void revisarFilasCompletas(RangoRecomendacionEdicionSolicitud solicitud) {
        Set<RangoRecomendacionCategoria> llegaron = EnumSet.noneOf(RangoRecomendacionCategoria.class);
        solicitud.filas().forEach(fila -> llegaron.add(fila.categoria()));
        int esperadas = RangoRecomendacionCategoria.values().length;
        if (solicitud.filas().size() != esperadas || llegaron.size() != esperadas) {
            throw new ReglaIncumplidaExcepcion(
                    List.of(new CampoInvalido("filas", "rango_recomendacion.filas_completas")));
        }
    }

    /**
     * Los datos de solo lectura no cambian: {@code limiteInferior} en la fila de {@code FUERA_DE_NORMA}.
     * Si cambia alguno, responde 400 en el campo de su fila.
     */
    private static void revisarSoloLectura(RangoRecomendacionEdicionSolicitud solicitud,
            Map<RangoRecomendacionCategoria, RangoRecomendacion> filas) {
        List<CampoInvalido> cambiados = new ArrayList<>();
        for (int i = 0; i < solicitud.filas().size(); i++) {
            RangoRecomendacionFilaSolicitud fila = solicitud.filas().get(i);
            RangoRecomendacion guardada = filas.get(fila.categoria());
            if (fila.categoria() == RangoRecomendacionCategoria.FUERA_DE_NORMA
                    && distintos(fila.limiteInferior(), guardada.getLimiteInferior())) {
                cambiados.add(new CampoInvalido("filas[" + i + "].limiteInferior",
                        "rango_recomendacion.limite_inferior.solo_lectura_si"));
            }
        }
        if (!cambiados.isEmpty()) {
            throw new ReglaIncumplidaExcepcion(cambiados);
        }
    }

    /** Si dos números son distintos, sin contar los ceros del final: 0 y 0.0 son el mismo. */
    private static boolean distintos(BigDecimal uno, BigDecimal otro) {
        return uno == null || otro == null ? uno != otro : uno.compareTo(otro) != 0;
    }

    private void revisarReglasAlEditar(List<RangoRecomendacion> filas, UsuarioEnSesion sesion) {
        List<CampoInvalido> incumplidas = new ArrayList<>();
        if (!reglas.cumpleLimitesEnOrden(filas, sesion)) {
            incumplidas.add(new CampoInvalido(null, "rango_recomendacion.reglas.limites_en_orden"));
        }
        if (!incumplidas.isEmpty()) {
            throw new ReglaIncumplidaExcepcion(incumplidas);
        }
    }

    /** Las respuestas de las filas, en el orden de la lista. */
    private static List<RangoRecomendacionRespuesta> respuestas(
            Map<RangoRecomendacionCategoria, RangoRecomendacion> filas) {
        return filas.values().stream().map(RangoRecomendacionMapeador::respuesta).toList();
    }
}
