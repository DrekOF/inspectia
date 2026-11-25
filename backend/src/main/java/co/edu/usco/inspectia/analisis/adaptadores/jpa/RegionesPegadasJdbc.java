/*
 * Nombre: RegionesPegadasJdbc.java
 * Descripción: La tabla region_pegada con SQL: guardar, reemplazar y leer las regiones de granos pegados de las fotos;
 *              siempre filtra por la empresa (RF-07 C2, D-45, RNF-03).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.aplicacion.Rectangulo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Las regiones de granos pegados de las fotos (D-45), con SQL en la transacción de Spring: no tienen entidad, porque
 * solo se guardan juntas al revisar la foto y se leen para mostrarlas mientras la foto está por repetir. Se borran con
 * su foto ({@code ON DELETE CASCADE}).
 */
@Component
public class RegionesPegadasJdbc {

    private static final String BORRAR = "DELETE FROM region_pegada WHERE empresa_id = ? AND foto_id = ?";

    private static final String INSERTAR =
            "INSERT INTO region_pegada (empresa_id, foto_id, x, y, ancho, alto) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String DE_LAS_FOTOS = "SELECT foto_id, x, y, ancho, alto FROM region_pegada "
            + "WHERE empresa_id = :empresa AND foto_id IN (:fotos) ORDER BY foto_id, id";

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate conNombres;

    /**
     * Crea el acceso; Spring le pasa el de la base.
     *
     * @param jdbc el acceso a la base con SQL
     */
    public RegionesPegadasJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.conNombres = new NamedParameterJdbcTemplate(jdbc);
    }

    /**
     * Reemplaza las regiones de una foto: borra las que tuviera y guarda estas.
     *
     * @param empresa la empresa
     * @param foto la foto
     * @param regiones las regiones nuevas; vacía para dejarla sin regiones
     */
    public void reemplazar(UUID empresa, UUID foto, List<Rectangulo> regiones) {
        jdbc.update(BORRAR, empresa, foto);
        if (!regiones.isEmpty()) {
            jdbc.batchUpdate(INSERTAR, regiones, regiones.size(), (sentencia, region) -> {
                sentencia.setObject(1, empresa);
                sentencia.setObject(2, foto);
                sentencia.setInt(3, region.x());
                sentencia.setInt(4, region.y());
                sentencia.setInt(5, region.ancho());
                sentencia.setInt(6, region.alto());
            });
        }
    }

    /**
     * Las regiones de unas fotos de la empresa, en el orden en que se guardaron.
     *
     * @param empresa la empresa
     * @param fotos las fotos
     * @return las regiones de cada foto que tiene
     */
    public Map<UUID, List<Rectangulo>> deLasFotos(UUID empresa, Collection<UUID> fotos) {
        Map<UUID, List<Rectangulo>> regiones = new LinkedHashMap<>();
        if (fotos.isEmpty()) {
            return regiones;
        }
        conNombres.query(DE_LAS_FOTOS, new MapSqlParameterSource("empresa", empresa).addValue("fotos", fotos),
                fila -> {
                    regiones.computeIfAbsent(fila.getObject("foto_id", UUID.class), foto -> new ArrayList<>())
                            .add(new Rectangulo(fila.getInt("x"), fila.getInt("y"), fila.getInt("ancho"),
                                    fila.getInt("alto")));
                });
        return regiones;
    }
}
