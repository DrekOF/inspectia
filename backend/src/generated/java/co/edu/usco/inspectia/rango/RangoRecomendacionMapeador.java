/*
 * Nombre: RangoRecomendacionMapeador.java
 * Descripción: Conversión entre rango_recomendacion y sus filas y respuesta.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.6)
 * Versión: 1.6
 * Entidades: rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.rango;

import co.edu.usco.inspectia.comun.numero.Decimales;

/** Conversión entre {@link RangoRecomendacion} y sus solicitudes y respuesta. Solo copia datos. */
public final class RangoRecomendacionMapeador {

    private RangoRecomendacionMapeador() {
    }

    /**
     * Copia en el registro los datos de la solicitud.
     * Los decimales quedan con la escala de su columna, como los guarda la base.
     *
     * @param solicitud los datos validados
     * @param rangoRecomendacion el registro que se edita
     */
    public static void aplicar(RangoRecomendacionFilaSolicitud solicitud, RangoRecomendacion rangoRecomendacion) {
        rangoRecomendacion.setLimiteInferior(Decimales.conEscala(solicitud.limiteInferior(), 1));
        rangoRecomendacion.setRecomendacion(solicitud.recomendacion());
    }

    /**
     * Copia un registro sin ligarlo a la base: sirve para revisar los cambios antes de aplicarlos.
     *
     * @param original el registro guardado
     * @return la copia
     */
    public static RangoRecomendacion copia(RangoRecomendacion original) {
        RangoRecomendacion copia = new RangoRecomendacion();
        copia.setId(original.getId());
        copia.setEmpresaId(original.getEmpresaId());
        copia.setCategoria(original.getCategoria());
        copia.setLimiteInferior(original.getLimiteInferior());
        copia.setRecomendacion(original.getRecomendacion());
        copia.setFechaCreacion(original.getFechaCreacion());
        copia.setFechaActualizacion(original.getFechaActualizacion());
        return copia;
    }

    /**
     * La respuesta de la API para un registro.
     *
     * @param rangoRecomendacion el registro
     * @return la respuesta
     */
    public static RangoRecomendacionRespuesta respuesta(RangoRecomendacion rangoRecomendacion) {
        return new RangoRecomendacionRespuesta(
                rangoRecomendacion.getId(),
                rangoRecomendacion.getCategoria(),
                rangoRecomendacion.getLimiteInferior(),
                rangoRecomendacion.getRecomendacion(),
                rangoRecomendacion.getFechaCreacion(),
                rangoRecomendacion.getFechaActualizacion());
    }
}
