/*
 * Nombre: RepositorioDeLotes.java
 * Descripción: Puerto de los lotes guardados: registrar, revisar el código, consultar el historial con sus filtros y
 *              quienes registraron lotes, saber si sugiere otra muestra, guardar y consultar el resultado, y leer y
 *              guardar la base de su reporte PDF con el nombre de su empresa (RF-04, RF-10, RF-12, RF-14, RF-15,
 *              D-25, D-47, D-50).
 * Fecha de creación: 10/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.4
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.analisis.dominio.Lote;
import co.edu.usco.inspectia.analisis.dominio.ResultadoDelLote;
import java.time.Instant;
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
     * Una página del historial: los lotes de la empresa que cumplen todos los filtros, del último movimiento al más
     * antiguo. El último movimiento es la fecha del resultado si el lote ya se analizó, o la de registro si está
     * pendiente. El total de la página cuenta los mismos lotes.
     *
     * @param empresaId la empresa
     * @param filtros los filtros (RF-14 C2); los que van en {@code null} no filtran
     * @param pagina qué página y de qué tamaño
     * @return la página
     */
    Page<VistaDeLote> historial(UUID empresaId, FiltrosDelHistorial filtros, Pageable pagina);

    /**
     * Quienes registraron lotes de la empresa, una vez cada uno, también los desactivados (RF-14 restricción 6).
     *
     * @param empresaId la empresa
     * @return su identificador y su nombre, sin orden
     */
    List<Registrador> registradores(UUID empresaId);

    /**
     * Las formas en que la empresa escribió sus fincas y proveedores, cada una con cuántos lotes la usan y la
     * última vez que se usó.
     *
     * @param empresaId la empresa
     * @return las formas, sin orden
     */
    List<OrigenUsado> origenesUsados(UUID empresaId);

    /**
     * El estado de un lote de la empresa.
     *
     * @param empresaId la empresa
     * @param id el lote
     * @return el estado, o vacío si el lote no existe o es de otra empresa
     */
    Optional<EstadoDeLote> estado(UUID empresaId, UUID id);

    /**
     * El estado de un lote de la empresa, bloqueando su fila hasta el final de la transacción: así dos fotos que se
     * confirman a la vez no crean dos muestras ni se pisan (RF-05), y confirmar una foto, enviar la muestra y guardar
     * su resultado se ordenan entre sí, siempre con el lote primero.
     *
     * @param empresaId la empresa
     * @param id el lote
     * @return el estado, o vacío si el lote no existe o es de otra empresa
     */
    Optional<EstadoDeLote> estadoParaActualizar(UUID empresaId, UUID id);

    /**
     * Si el resultado guardado del lote sugiere analizar otra muestra (D-24). Sigue en {@code true} mientras se toma y
     * se analiza la muestra 2, y queda en {@code false} cuando termina (D-25). Dentro de una transacción que ya bloqueó
     * el lote ({@link #estadoParaActualizar}), lee el valor bloqueado.
     *
     * @param empresaId la empresa
     * @param id el lote
     * @return {@code true} si lo sugiere; {@code false} si no, si está pendiente, o si no existe o es de otra empresa
     */
    boolean sugiereOtraMuestra(UUID empresaId, UUID id);

    /**
     * Guarda el resultado de un lote y lo deja analizado (RF-10 C6, RF-12). Se llama con el lote ya bloqueado. Con la
     * muestra 2, reemplaza el resultado de la 1 (D-25), y en los dos casos el lote olvida la ruta de su reporte, que
     * se rehace con el resultado nuevo (RF-15).
     *
     * @param empresaId la empresa
     * @param id el lote
     * @param resultado el porcentaje corregido, su margen, la categoría, la recomendación, la sugerencia con su límite
     *     y su categoría (D-47), y el aviso
     * @param fechaResultado la fecha del resultado, al microsegundo
     */
    void guardarResultado(UUID empresaId, UUID id, ResultadoDelLote resultado, Instant fechaResultado);

    /**
     * El resultado guardado de un lote de la empresa, sin recalcularlo (RF-14 C3).
     *
     * @param empresaId la empresa
     * @param id el lote
     * @return el resultado, o vacío si el lote no existe o es de otra empresa
     */
    Optional<ResultadoGuardado> resultado(UUID empresaId, UUID id);

    /**
     * La empresa de un lote, como está hoy, y la base de su reporte PDF, si ya se armó (RF-15, D-50).
     *
     * @param empresaId la empresa
     * @param id el lote
     * @return los datos, o vacío si el lote no existe o es de otra empresa
     */
    Optional<DatosDelReporte> datosDelReporte(UUID empresaId, UUID id);

    /**
     * Guarda la base de los archivos del reporte PDF de un lote analizado (RF-15 C4, D-50). Se llama con el lote ya
     * bloqueado.
     *
     * @param empresaId la empresa
     * @param id el lote
     * @param base la base, {@code reportes/<empresa>/<lote>/<uuid>}
     */
    void guardarRutaDelReporte(UUID empresaId, UUID id, String base);
}
