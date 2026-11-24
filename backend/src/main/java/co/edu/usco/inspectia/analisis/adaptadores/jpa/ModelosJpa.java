/*
 * Nombre: ModelosJpa.java
 * Descripción: Repositorio de Spring Data de la tabla modelo: buscar un modelo por su nombre y su versión, y registrar
 *              uno si falta (RF-08, D-39).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** La tabla {@code modelo}, común a todas las empresas (RNF-03). */
public interface ModelosJpa extends JpaRepository<ModeloEntidad, UUID> {

    /**
     * Un modelo por su nombre y su versión, que son únicos juntos.
     *
     * @param nombre el nombre
     * @param version la versión
     * @return el modelo, o vacío si no está
     */
    @Query("select m from ModeloEntidad m where m.nombre = :nombre and m.version = :version")
    Optional<ModeloEntidad> porNombreYVersion(@Param("nombre") String nombre, @Param("version") String version);

    /**
     * Registra un modelo sin las métricas de RF-17 y sin ponerlo en uso; si ya hay uno con ese nombre y esa versión, no
     * lo cambia. Pide una transacción.
     *
     * @param nombre el nombre
     * @param version la versión
     * @param recallSano el recall de la clase sano, en porcentaje
     * @param recallNoSano el recall de la clase no sano, en porcentaje
     * @return 1 si lo registró, 0 si ya estaba
     */
    @Modifying
    @Query(value = "INSERT INTO modelo (nombre, version, recall_sano, recall_no_sano, en_uso) "
            + "VALUES (:nombre, :version, :recallSano, :recallNoSano, false) ON CONFLICT (nombre, version) DO NOTHING",
            nativeQuery = true)
    int registrarSiFalta(@Param("nombre") String nombre, @Param("version") String version,
            @Param("recallSano") BigDecimal recallSano, @Param("recallNoSano") BigDecimal recallNoSano);
}
