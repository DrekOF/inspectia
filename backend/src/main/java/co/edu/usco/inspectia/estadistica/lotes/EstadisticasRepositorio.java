/*
 * Nombre: EstadisticasRepositorio.java
 * Descripción: Lee de la tabla lote los lotes analizados de una empresa en un periodo de la fecha del resultado, para
 *              las estadísticas (RF-16, RNF-03).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.lotes;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Los lotes analizados de una empresa en un periodo (RF-16). Lee la tabla {@code lote} con SQL y no con las entidades
 * del módulo de análisis: este módulo va por capas y solo necesita 4 columnas de los lotes ya analizados. Siempre
 * filtra por la empresa que recibe (RNF-03).
 */
@Repository
public class EstadisticasRepositorio {

    private final JdbcClient jdbc;

    /**
     * Crea el repositorio; Spring le pasa el cliente de JDBC.
     *
     * @param jdbc el cliente de JDBC
     */
    public EstadisticasRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Los lotes analizados de la empresa con la fecha del resultado en el periodo. Es la misma condición del historial
     * filtrado por «Analizado» y esas fechas (RF-14 C2), así las cifras coinciden con él (RF-16 C1).
     *
     * @param empresaId la empresa de la sesión
     * @param inicio el primer instante que entra, o {@code null} si el periodo no tiene inicio
     * @param fin el primer instante que ya no entra, o {@code null} si el periodo no tiene fin
     * @return los lotes, sin orden
     */
    public List<LoteAnalizado> analizados(UUID empresaId, Instant inicio, Instant fin) {
        StringBuilder sql = new StringBuilder("""
                SELECT origen, porcentaje_sanos, categoria, fecha_resultado
                FROM lote
                WHERE empresa_id = :empresa AND estado = 'ANALIZADO'""");
        // Las condiciones de las fechas solo van si el periodo las tiene: un parámetro nulo no tiene tipo en PostgreSQL.
        if (inicio != null) {
            sql.append(" AND fecha_resultado >= :inicio");
        }
        if (fin != null) {
            sql.append(" AND fecha_resultado < :fin");
        }
        JdbcClient.StatementSpec consulta = jdbc.sql(sql.toString()).param("empresa", empresaId);
        if (inicio != null) {
            consulta = consulta.param("inicio", OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC));
        }
        if (fin != null) {
            consulta = consulta.param("fin", OffsetDateTime.ofInstant(fin, ZoneOffset.UTC));
        }
        return consulta.query((fila, numero) -> new LoteAnalizado(fila.getString("origen"),
                fila.getBigDecimal("porcentaje_sanos"), fila.getString("categoria"),
                fila.getObject("fecha_resultado", OffsetDateTime.class).toInstant())).list();
    }
}
