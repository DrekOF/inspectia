/*
 * Nombre: EstadisticasServicio.java
 * Descripción: Las estadísticas de la empresa en un periodo (RF-16): valida el periodo como los filtros del historial,
 *              lee los lotes analizados de la empresa de la sesión y arma las cifras.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.lotes;

import co.edu.usco.inspectia.analisis.aplicacion.FiltrosDelHistorial;
import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las estadísticas de la empresa de la sesión (RF-16, RNF-03). El periodo se interpreta con
 * {@link FiltrosDelHistorial}, los mismos filtros del historial: días de Colombia con las dos fechas incluidas y de los
 * años 0001 a 9999. Así un periodo trae exactamente los lotes analizados que muestra el historial con esas fechas
 * (RF-16 C1).
 */
@Service
@Transactional(readOnly = true)
public class EstadisticasServicio {

    private final EstadisticasRepositorio repositorio;

    /**
     * Crea el servicio; Spring le pasa el repositorio.
     *
     * @param repositorio los lotes analizados
     */
    public EstadisticasServicio(EstadisticasRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Las estadísticas de la empresa en el periodo.
     *
     * @param empresaId la empresa de la sesión
     * @param desde el primer día del periodo, incluido, o {@code null} para no limitarlo
     * @param hasta el último día del periodo, incluido, o {@code null} para no limitarlo
     * @return las estadísticas
     * @throws ReglaIncumplidaExcepcion si una fecha se sale de los años 0001 a 9999 (400 en su campo), o si «Hasta» es
     *     anterior a «Desde» (400 en {@code hasta})
     */
    public Estadisticas calcular(UUID empresaId, LocalDate desde, LocalDate hasta) {
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
        return CalculoDeEstadisticas.calcular(desde, hasta,
                repositorio.analizados(empresaId, periodo.inicio(), periodo.fin()));
    }
}
