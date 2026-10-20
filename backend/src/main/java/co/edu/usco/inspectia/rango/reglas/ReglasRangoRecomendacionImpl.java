/*
 * Nombre: ReglasRangoRecomendacionImpl.java
 * Descripción: Reglas de rango_recomendacion escritas a mano: implementa la interfaz generada
 *              ReglasRangoRecomendacion (RP-23, RF-13).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.rango.reglas;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import co.edu.usco.inspectia.rango.RangoRecomendacion;
import co.edu.usco.inspectia.rango.ReglasRangoRecomendacion;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Reglas de {@code specs/entidades/rango_recomendacion.json} que se programan a mano. El servicio generado
 * las llama al editar los rangos de la empresa de la sesión (RF-13).
 */
@Component
public class ReglasRangoRecomendacionImpl implements ReglasRangoRecomendacion {

    /**
     * Los límites bajan estrictamente en el orden de las categorías del JSON, de la mejor a la peor: Premium
     * o Especial &gt; Estándar &gt; Corriente &gt; Fuera de norma, que es fijo en 0 % (RF-13 C1). Un empate
     * no cumple: un lote con ese porcentaje no tendría una sola categoría.
     *
     * <p>Compara en el orden de las categorías, aunque las filas lleguen en otro, y por valor: 90 y 90.0
     * son el mismo límite.
     */
    @Override
    public boolean cumpleLimitesEnOrden(List<RangoRecomendacion> filas, UsuarioEnSesion sesion) {
        List<RangoRecomendacion> enOrden = filas.stream()
                .sorted(Comparator.comparing(RangoRecomendacion::getCategoria))
                .toList();
        for (int i = 1; i < enOrden.size(); i++) {
            if (enOrden.get(i).getLimiteInferior().compareTo(enOrden.get(i - 1).getLimiteInferior()) >= 0) {
                return false;
            }
        }
        return true;
    }
}
