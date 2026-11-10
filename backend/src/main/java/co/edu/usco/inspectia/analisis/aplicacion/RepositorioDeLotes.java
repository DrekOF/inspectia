/*
 * Nombre: RepositorioDeLotes.java
 * Descripción: Puerto de los lotes guardados: registrar, revisar el código y consultar el historial (RF-04,
 *              RF-14).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.Lote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de los lotes guardados (D-21). Todas las consultas reciben la empresa de la sesión y solo
 * responden lotes de ella (RNF-03). Lo implementa el adaptador de JPA.
 */
public interface RepositorioDeLotes {

    /**
     * Si la empresa ya tiene un lote con ese código.
     *
     * @param empresaId la empresa
     * @param claveDeCodigo la clave del código ({@link Lote#claveDeCodigo})
     * @return {@code true} si ya existe
     */
    boolean existeCodigo(UUID empresaId, String claveDeCodigo);

    /**
     * Guarda un lote nuevo.
     *
     * @param lote el lote
     * @return el lote guardado, con su identificador y su fecha de registro
     */
    VistaDeLote guardar(Lote lote);

    /**
     * Un lote de la empresa.
     *
     * @param empresaId la empresa
     * @param id el lote
     * @return el lote, o vacío si no existe o es de otra empresa
     */
    Optional<VistaDeLote> buscar(UUID empresaId, UUID id);

    /**
     * Una página del historial: los lotes de la empresa, del último movimiento al más antiguo. El último
     * movimiento es la fecha del resultado si el lote ya se analizó, o la de registro si está pendiente.
     *
     * @param empresaId la empresa
     * @param pagina qué página y de qué tamaño
     * @return la página
     */
    Page<VistaDeLote> historial(UUID empresaId, Pageable pagina);

    /**
     * Las formas en que la empresa escribió sus fincas y proveedores, cada una con la última vez que la usó.
     *
     * @param empresaId la empresa
     * @return las formas, sin orden
     */
    List<OrigenUsado> origenesUsados(UUID empresaId);
}
