/*
 * Nombre: ConsultarAnalisis.java
 * Descripción: Caso de uso de consultar el avance del análisis de la muestra en curso de un lote, para el indicador de
 *              avance de la página (RNF-01 C4, RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El avance del análisis de un lote (RNF-01 C4, RNF-02 C3): la página lo pregunta cada segundo mientras espera. Es
 * liviano: no firma enlaces ni lee granos. Solo responde lotes de la empresa de la sesión (RNF-03).
 */
@Service
@Transactional(readOnly = true)
public class ConsultarAnalisis {

    private final RepositorioDeLotes lotes;
    private final RepositorioDeMuestras muestras;

    /**
     * Crea el caso de uso; Spring le pasa sus dependencias.
     *
     * @param lotes los lotes guardados
     * @param muestras las muestras
     */
    public ConsultarAnalisis(RepositorioDeLotes lotes, RepositorioDeMuestras muestras) {
        this.lotes = lotes;
        this.muestras = muestras;
    }

    /**
     * El avance de la muestra en curso del lote.
     *
     * @param empresaId la empresa de la sesión
     * @param loteId el lote
     * @return el avance; vacío, sin muestra, si el lote todavía no tiene fotos
     * @throws NoEncontradoExcepcion si el lote no existe o es de otra empresa (404, RNF-03 C1)
     */
    public AvanceDelAnalisis avance(UUID empresaId, UUID loteId) {
        if (lotes.estado(empresaId, loteId).isEmpty()) {
            throw new NoEncontradoExcepcion("lote " + loteId);
        }
        return muestras.avance(empresaId, loteId).orElseGet(AvanceDelAnalisis::sinMuestra);
    }
}
