/*
 * Nombre: GranosJpa.java
 * Descripción: Repositorio de Spring Data de la tabla grano: leer y borrar los granos de unas fotos; siempre filtra por
 *              la empresa (RNF-03, RF-09).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** La tabla {@code grano}. Las consultas reciben la empresa (RNF-03). */
public interface GranosJpa extends JpaRepository<GranoEntidad, UUID> {

    /**
     * Los granos de unas fotos de la empresa, en el orden en que se guardaron: su identificador UUID v7 crece con cada
     * grano que guarda el servidor.
     *
     * @param empresa la empresa
     * @param fotos las fotos; al menos una
     * @return los granos
     */
    @Query("select g from GranoEntidad g where g.empresaId = :empresa and g.fotoId in :fotos order by g.id")
    List<GranoEntidad> deLasFotos(@Param("empresa") UUID empresa, @Param("fotos") Collection<UUID> fotos);

    /**
     * Borra los granos de unas fotos de la empresa, como los de la foto que RF-09 pidió repetir. Manda antes a la base
     * lo pendiente de la transacción.
     *
     * @param empresa la empresa
     * @param fotos las fotos; al menos una
     * @return cuántos borró
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from GranoEntidad g where g.empresaId = :empresa and g.fotoId in :fotos")
    int borrarDeLasFotos(@Param("empresa") UUID empresa, @Param("fotos") Collection<UUID> fotos);
}
