/*
 * Nombre: ConfianzaDeLosAnalisisRepositorio.java
 * Descripción: La confianza promedio de los granos de los análisis de una empresa, con el periodo opcional por la fecha
 *              en que terminó cada muestra (RF-17 C2, RNF-03).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * La confianza promedio de los análisis de una empresa (RF-17 C2). Lee los granos con SQL, como las estadísticas: las
 * condiciones de las fechas solo van si el periodo las tiene, porque un parámetro nulo no tiene tipo en PostgreSQL.
 * Siempre filtra por la empresa que recibe (RNF-03).
 */
@Repository
public class ConfianzaDeLosAnalisisRepositorio {

    private final JdbcClient jdbc;

    /**
     * Crea el repositorio; Spring le pasa el cliente de JDBC.
     *
     * @param jdbc el cliente de JDBC
     */
    public ConfianzaDeLosAnalisisRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * La confianza promedio de los granos de los análisis de una empresa, redondeada a 2 decimales, con cuántos granos
     * y de cuántas muestras. El análisis solo guarda los granos de una muestra cuando termina: los de una muestra
     * devuelta o en curso no están. Es de los análisis de la empresa con cualquier modelo; con un periodo, solo de las
     * muestras que terminaron en él.
     *
     * @param empresa la empresa de la sesión
     * @param inicio el primer instante que entra, o {@code null} si el periodo no tiene inicio
     * @param fin el primer instante que ya no entra, o {@code null} si el periodo no tiene fin
     * @return una fila; sin granos, el promedio va vacío y los conteos en 0
     */
    public ConfianzaDeLaEmpresa confianzaDe(UUID empresa, Instant inicio, Instant fin) {
        StringBuilder sql = new StringBuilder("""
                SELECT round(avg(g.confianza), 2) AS promedio, count(g.id) AS granos,
                    count(DISTINCT f.muestra_id) AS muestras
                FROM grano g
                    JOIN foto f ON f.id = g.foto_id AND f.empresa_id = g.empresa_id
                    JOIN muestra m ON m.id = f.muestra_id AND m.empresa_id = f.empresa_id
                WHERE g.empresa_id = :empresa""");
        if (inicio != null) {
            sql.append(" AND m.fecha_fin >= :inicio");
        }
        if (fin != null) {
            sql.append(" AND m.fecha_fin < :fin");
        }
        JdbcClient.StatementSpec consulta = jdbc.sql(sql.toString()).param("empresa", empresa);
        if (inicio != null) {
            consulta = consulta.param("inicio", OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC));
        }
        if (fin != null) {
            consulta = consulta.param("fin", OffsetDateTime.ofInstant(fin, ZoneOffset.UTC));
        }
        return consulta.query((fila, numero) -> new Fila(fila.getBigDecimal("promedio"), fila.getLong("granos"),
                fila.getLong("muestras"))).single();
    }

    /** Una fila de la consulta. */
    private record Fila(BigDecimal promedio, long granos, long muestras) implements ConfianzaDeLaEmpresa {

        @Override
        public BigDecimal getPromedio() {
            return promedio;
        }

        @Override
        public Long getGranos() {
            return granos;
        }

        @Override
        public Long getMuestras() {
            return muestras;
        }
    }
}
