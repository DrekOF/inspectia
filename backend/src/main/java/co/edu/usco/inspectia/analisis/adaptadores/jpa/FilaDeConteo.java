/*
 * Nombre: FilaDeConteo.java
 * Descripción: El conteo de una muestra terminada como lo lee JPA, con los recall de su modelo (RF-10, D-39).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.dominio.ConteoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.RecallsDelModelo;
import java.math.BigDecimal;

/**
 * Una fila de {@link MuestrasJpa#terminadas}; {@link #conteo()} la pasa al dominio.
 *
 * @param granosSanos los granos sanos de la muestra
 * @param totalGranos su total de granos
 * @param recallSano el recall de sano de su modelo
 * @param recallNoSano el recall de no sano de su modelo
 * @param avisoConfianzaBaja si salió con el aviso de confianza baja
 */
public record FilaDeConteo(Integer granosSanos, Integer totalGranos, BigDecimal recallSano, BigDecimal recallNoSano,
        Boolean avisoConfianzaBaja) {

    /** @return el conteo para el dominio */
    ConteoDeMuestra conteo() {
        return new ConteoDeMuestra(granosSanos, totalGranos, new RecallsDelModelo(recallSano, recallNoSano),
                Boolean.TRUE.equals(avisoConfianzaBaja));
    }
}
