/*
 * Nombre: PeriodoDeConsulta.java
 * Descripción: El periodo opcional de las consultas del módulo de estadísticas (RF-16 y RF-17): «Desde» y «Hasta» en
 *              días de Colombia, con las dos fechas incluidas, como los filtros del historial, y sus validaciones.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica;

import co.edu.usco.inspectia.analisis.aplicacion.FiltrosDelHistorial;
import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import java.time.LocalDate;
import java.util.List;

/**
 * El periodo de una consulta de estadísticas. Se interpreta con {@link FiltrosDelHistorial}, los mismos filtros del
 * historial: días de Colombia con las dos fechas incluidas y de los años 0001 a 9999. Las estadísticas (RF-16) y la
 * confianza promedio del modelo (RF-17) lo validan igual.
 */
public final class PeriodoDeConsulta {

    private PeriodoDeConsulta() {
    }

    /**
     * El periodo, ya validado.
     *
     * @param desde el primer día, incluido, o {@code null} para no limitarlo
     * @param hasta el último día, incluido, o {@code null} para no limitarlo
     * @return el periodo, con su primer instante ({@code inicio()}) y el primero que ya no entra ({@code fin()})
     * @throws ReglaIncumplidaExcepcion si una fecha se sale de los años 0001 a 9999 (400 en su campo), o si «Hasta» es
     *     anterior a «Desde» (400 en {@code hasta})
     */
    public static FiltrosDelHistorial validado(LocalDate desde, LocalDate hasta) {
        FiltrosDelHistorial periodo = new FiltrosDelHistorial(desde, hasta, null, null, null, null);
        // El rango va primero: con un año fuera de él, «Hasta antes de desde» no sería el motivo.
        List<CampoInvalido> fueraDeRango = periodo.fechasFueraDeRango().stream()
                .map(campo -> new CampoInvalido(campo, "error.valor_invalido"))
                .toList();
        if (!fueraDeRango.isEmpty()) {
            throw new ReglaIncumplidaExcepcion(fueraDeRango);
        }
        if (periodo.fechasAlReves()) {
            throw new ReglaIncumplidaExcepcion(
                    List.of(new CampoInvalido("hasta", "estadistica.hasta_antes_de_desde")));
        }
        return periodo;
    }
}
