/*
 * Nombre: RangoRecomendacionValoresIniciales.java
 * Descripción: Filas por defecto de rango_recomendacion para cada empresa nueva.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.4)
 * Versión: 1.4
 * Entidades: rango_recomendacion 1.1
 */
package co.edu.usco.inspectia.rango;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Filas por defecto de {@code rango_recomendacion}. Se crean al registrar cada empresa (RF-02) y «Restaurar valores por defecto» vuelve a ellos (RF-13).
 * Fuente: anexo A.3 del documento de requerimientos (D-10).
 */
public final class RangoRecomendacionValoresIniciales {

    private RangoRecomendacionValoresIniciales() {
    }

    /**
     * Las filas por defecto de una empresa nueva, listas para guardar.
     *
     * @param empresaId la empresa nueva
     * @return las filas, en el orden del JSON
     */
    public static List<RangoRecomendacion> paraEmpresa(UUID empresaId) {
        return List.of(
                fila(empresaId,
                        RangoRecomendacionCategoria.PREMIUM,
                        new BigDecimal("96.0"),
                        "Apto para mercado especial o de exportación."),
                fila(empresaId,
                        RangoRecomendacionCategoria.ESTANDAR,
                        new BigDecimal("90.0"),
                        "Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de categoría."),
                fila(empresaId,
                        RangoRecomendacionCategoria.CORRIENTE,
                        new BigDecimal("82.0"),
                        "Retirar los granos defectuosos antes de vender, y revisar la fermentación y el secado."),
                fila(empresaId,
                        RangoRecomendacionCategoria.FUERA_DE_NORMA,
                        new BigDecimal("0.0"),
                        "No venderlo como cacao de calidad: hacer una selección completa y revisar todo el proceso después de la cosecha."));
    }

    private static RangoRecomendacion fila(UUID empresaId, RangoRecomendacionCategoria categoria, BigDecimal limiteInferior, String recomendacion) {
        RangoRecomendacion fila = new RangoRecomendacion();
        fila.setEmpresaId(empresaId);
        fila.setCategoria(categoria);
        fila.setLimiteInferior(limiteInferior);
        fila.setRecomendacion(recomendacion);
        return fila;
    }
}
