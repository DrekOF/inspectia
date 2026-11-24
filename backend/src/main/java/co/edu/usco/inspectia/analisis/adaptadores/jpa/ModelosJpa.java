/*
 * Nombre: ModelosJpa.java
 * Descripción: Repositorio de Spring Data de la tabla modelo: buscar un modelo por su nombre y su versión, y registrar
 *              uno si falta (RF-08, D-39).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
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

    /**
     * Registra un modelo de la comparación con todas sus métricas y sin ponerlo en uso; si ya hay uno con ese nombre y
     * esa versión, no lo cambia (D-18, D-48). Pide una transacción.
     *
     * @param nombre el nombre
     * @param version la versión
     * @param tipo CNN o TRANSFORMER
     * @param f1Macro el F1 macro
     * @param precisionMacro la precisión macro
     * @param recallMacro el recall macro
     * @param recallSano el recall de sano
     * @param recallNoSano el recall de no sano
     * @param precisionSano la precisión de sano
     * @param precisionNoSano la precisión de no sano
     * @param sanosComoSanos sanos clasificados como sanos
     * @param sanosComoNoSanos sanos clasificados como no sanos
     * @param noSanosComoSanos no sanos clasificados como sanos
     * @param noSanosComoNoSanos no sanos clasificados como no sanos
     * @param tiempoPorGranoMs el tiempo por grano, en milisegundos
     * @return 1 si lo registró, 0 si ya estaba
     */
    @Modifying
    @Query(value = "INSERT INTO modelo (nombre, version, tipo, f1_macro, precision_macro, recall_macro, recall_sano, "
            + "recall_no_sano, precision_sano, precision_no_sano, sanos_como_sanos, sanos_como_no_sanos, "
            + "no_sanos_como_sanos, no_sanos_como_no_sanos, tiempo_por_grano_ms, en_uso) "
            + "VALUES (:nombre, :version, :tipo, :f1Macro, :precisionMacro, :recallMacro, :recallSano, :recallNoSano, "
            + ":precisionSano, :precisionNoSano, :sanosComoSanos, :sanosComoNoSanos, :noSanosComoSanos, "
            + ":noSanosComoNoSanos, :tiempoPorGranoMs, false) ON CONFLICT (nombre, version) DO NOTHING",
            nativeQuery = true)
    int registrarConMetricas(@Param("nombre") String nombre, @Param("version") String version,
            @Param("tipo") String tipo, @Param("f1Macro") BigDecimal f1Macro,
            @Param("precisionMacro") BigDecimal precisionMacro, @Param("recallMacro") BigDecimal recallMacro,
            @Param("recallSano") BigDecimal recallSano, @Param("recallNoSano") BigDecimal recallNoSano,
            @Param("precisionSano") BigDecimal precisionSano, @Param("precisionNoSano") BigDecimal precisionNoSano,
            @Param("sanosComoSanos") int sanosComoSanos, @Param("sanosComoNoSanos") int sanosComoNoSanos,
            @Param("noSanosComoSanos") int noSanosComoSanos, @Param("noSanosComoNoSanos") int noSanosComoNoSanos,
            @Param("tiempoPorGranoMs") BigDecimal tiempoPorGranoMs);

    /**
     * Saca de uso a todos los modelos menos uno. Va antes de {@link #ponerEnUso}: el índice único de {@code en_uso} no
     * deja dos en uso ni por un momento. Pide una transacción.
     *
     * @param nombre el nombre del que queda
     * @param version su versión
     * @return cuántos sacó de uso
     */
    @Modifying
    @Query(value = "UPDATE modelo SET en_uso = false WHERE en_uso AND NOT (nombre = :nombre AND version = :version)",
            nativeQuery = true)
    int sacarDeUsoLosDemas(@Param("nombre") String nombre, @Param("version") String version);

    /**
     * Pone en uso un modelo. La base exige que tenga todas sus métricas y un F1 macro de 65 % o más (P-02). Pide una
     * transacción.
     *
     * @param nombre el nombre
     * @param version la versión
     * @return 1 si existe, 0 si no
     */
    @Modifying
    @Query(value = "UPDATE modelo SET en_uso = true WHERE nombre = :nombre AND version = :version",
            nativeQuery = true)
    int ponerEnUso(@Param("nombre") String nombre, @Param("version") String version);
}
