/*
 * Nombre: ColaEnLaBase.java
 * Descripción: El puerto ColaDeAnalisis en la tabla muestra: toma la muestra en espera más antigua con SELECT … FOR
 *              UPDATE SKIP LOCKED y devuelve a la cola las que quedaron en proceso (RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.aplicacion.ColaDeAnalisis;
import co.edu.usco.inspectia.analisis.aplicacion.MuestraTomada;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * La cola de análisis en la base (RNF-02 C3): son las muestras {@code EN_ESPERA}, ordenadas por {@code fecha_envio} y,
 * si empatan, por {@code id}, con el índice {@code muestra (estado, fecha_envio)}. No necesita columnas propias, y
 * ninguna muestra se pierde si el servidor se reinicia.
 *
 * <p>Tomar una muestra es una sola sentencia: la busca con {@code FOR UPDATE SKIP LOCKED}, así dos ejecuciones toman
 * muestras distintas, y la deja «en proceso» con su hora de inicio, que devuelve tal como la guardó la base. Usa SQL con
 * {@link JdbcTemplate}, en la transacción de Spring, porque JPQL no tiene {@code SKIP LOCKED} ni {@code RETURNING}.
 */
@Component
public class ColaEnLaBase implements ColaDeAnalisis {

    private static final String TOMAR = """
            UPDATE muestra SET estado = 'EN_PROCESO', fecha_inicio = ?
            WHERE estado = 'EN_ESPERA' AND id = (
                SELECT id FROM muestra WHERE estado = 'EN_ESPERA'
                ORDER BY fecha_envio, id
                LIMIT 1
                FOR UPDATE SKIP LOCKED)
            RETURNING id, empresa_id, lote_id, numero, repeticiones, fecha_inicio""";

    private static final String DEVOLVER_INTERRUMPIDAS =
            "UPDATE muestra SET estado = 'EN_ESPERA', fecha_inicio = NULL WHERE estado = 'EN_PROCESO'";

    private final JdbcTemplate jdbc;

    /**
     * Crea la cola; Spring le pasa el acceso a la base.
     *
     * @param jdbc el acceso a la base con SQL
     */
    public ColaEnLaBase(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public Optional<MuestraTomada> tomarLaSiguiente(Instant ahora) {
        return jdbc.query(TOMAR, (fila, indice) -> new MuestraTomada(
                        fila.getObject("id", UUID.class),
                        fila.getObject("empresa_id", UUID.class),
                        fila.getObject("lote_id", UUID.class),
                        fila.getInt("numero"),
                        fila.getInt("repeticiones"),
                        fila.getObject("fecha_inicio", OffsetDateTime.class).toInstant()),
                OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .stream().findFirst();
    }

    @Override
    @Transactional
    public int devolverLasInterrumpidas() {
        return jdbc.update(DEVOLVER_INTERRUMPIDAS);
    }
}
