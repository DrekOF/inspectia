/*
 * Nombre: MetricasDelModeloServicio.java
 * Descripción: Arma lo que ve el administrador de empresa en «Modelo de IA»: los modelos con sus métricas del
 *              conjunto de prueba y la confianza promedio de los análisis de su empresa, en todo su historial o
 *              en un periodo (RF-17, RNF-03).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.estadistica.modelo;

import co.edu.usco.inspectia.analisis.aplicacion.FiltrosDelHistorial;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import co.edu.usco.inspectia.estadistica.PeriodoDeConsulta;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las métricas del modelo de IA (RF-17). Las de prueba son las que guarda la tabla {@code modelo} de la comparación de
 * D-18 (C1), comunes a todas las empresas; la confianza promedio sale solo de los granos de la empresa de la sesión
 * (C2, RNF-03) y, si se pide, de las muestras que terminaron en un periodo, en días de Colombia como el historial y
 * las estadísticas ({@link PeriodoDeConsulta}).
 */
@Service
@Transactional(readOnly = true)
public class MetricasDelModeloServicio {

    private final MetricasDelModeloRepositorio repositorio;
    private final ConfianzaDeLosAnalisisRepositorio confianzas;

    /**
     * Crea el servicio; Spring le pasa los repositorios.
     *
     * @param repositorio las consultas de los modelos
     * @param confianzas la confianza promedio de los granos
     */
    public MetricasDelModeloServicio(MetricasDelModeloRepositorio repositorio,
            ConfianzaDeLosAnalisisRepositorio confianzas) {
        this.repositorio = repositorio;
        this.confianzas = confianzas;
    }

    /**
     * Las métricas de los modelos y la confianza promedio de una empresa.
     *
     * @param empresaId la empresa de la sesión
     * @param desde el primer día del periodo de la confianza, incluido, o {@code null} para no limitarlo
     * @param hasta el último día del periodo de la confianza, incluido, o {@code null} para no limitarlo
     * @return los modelos, del mayor F1 macro al menor, y la confianza promedio de sus análisis
     * @throws ReglaIncumplidaExcepcion si una fecha se sale de los años 0001 a 9999 (400 en su campo), o si «Hasta» es
     *     anterior a «Desde» (400 en {@code hasta})
     */
    public MetricasDelModeloRespuesta consultar(UUID empresaId, LocalDate desde, LocalDate hasta) {
        FiltrosDelHistorial periodo = PeriodoDeConsulta.validado(desde, hasta);
        return new MetricasDelModeloRespuesta(
                repositorio.comparados().stream().map(ModeloMedidoRespuesta::de).toList(),
                ConfianzaPromedioRespuesta.de(confianzas.confianzaDe(empresaId, periodo.inicio(), periodo.fin())));
    }
}
