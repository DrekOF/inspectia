/*
 * Nombre: ConsultarFotos.java
 * Descripción: Caso de uso de consultar las fotos de la muestra en curso de un lote, cada una con un enlace nuevo para
 *              verla, para retomar el lote (RF-05, RF-14 C5).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las fotos de la muestra en curso de un lote (RF-05, RF-14 C5): al retomarlo, la página ve las que ya se subieron.
 * Solo responde lotes de la empresa de la sesión (RNF-03).
 */
@Service
@Transactional(readOnly = true)
public class ConsultarFotos {

    private final RepositorioDeLotes lotes;
    private final RepositorioDeMuestras muestras;
    private final VistasDeFotos vistas;

    /**
     * Crea el caso de uso; Spring le pasa sus dependencias.
     *
     * @param lotes los lotes guardados
     * @param muestras las muestras y sus fotos
     * @param vistas las vistas de las fotos, con su enlace para verlas
     */
    public ConsultarFotos(RepositorioDeLotes lotes, RepositorioDeMuestras muestras, VistasDeFotos vistas) {
        this.lotes = lotes;
        this.muestras = muestras;
        this.vistas = vistas;
    }

    /**
     * Las fotos de la muestra en curso del lote.
     *
     * @param empresaId la empresa de la sesión
     * @param loteId el lote
     * @return la muestra y sus fotos, por número; sin muestra y sin fotos si todavía no se subió ninguna
     * @throws NoEncontradoExcepcion si el lote no existe o es de otra empresa (404, RNF-03 C1)
     */
    public FotosDeLaMuestra fotos(UUID empresaId, UUID loteId) {
        if (lotes.estado(empresaId, loteId).isEmpty()) {
            throw new NoEncontradoExcepcion("lote " + loteId);
        }
        return muestras.muestraEnCurso(empresaId, loteId)
                .map(muestra -> new FotosDeLaMuestra(muestra.numero(), muestra.estado(),
                        muestras.fotos(empresaId, muestra.id()).stream().map(vistas::de).toList()))
                .orElseGet(() -> new FotosDeLaMuestra(null, null, List.of()));
    }
}
