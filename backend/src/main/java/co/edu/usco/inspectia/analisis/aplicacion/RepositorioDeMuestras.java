/*
 * Nombre: RepositorioDeMuestras.java
 * Descripción: Puerto de las muestras de los lotes, de sus fotos y de sus granos (D-21, RF-05, RF-10, RNF-02). Todas
 *              las consultas reciben la empresa de la sesión o la de la muestra (RNF-03).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.ConteoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de las muestras, sus fotos y sus granos (D-21). Todas las consultas reciben la empresa de la sesión, o la de
 * la muestra que atiende la cola, y solo responden datos de ella (RNF-03). Lo implementa el adaptador de JPA.
 */
public interface RepositorioDeMuestras {

    /**
     * La muestra en curso del lote: la de número más alto.
     *
     * @param empresaId la empresa
     * @param loteId el lote
     * @return la muestra, o vacío si el lote todavía no tiene fotos
     */
    Optional<MuestraEnCurso> muestraEnCurso(UUID empresaId, UUID loteId);

    /**
     * Crea una muestra del lote, pendiente.
     *
     * @param empresaId la empresa
     * @param loteId el lote
     * @param numero su número: 1, o 2 para la segunda (D-25)
     * @param usuarioId quien la toma
     * @return la muestra
     */
    MuestraEnCurso crear(UUID empresaId, UUID loteId, int numero, UUID usuarioId);

    /**
     * Las fotos de una muestra, por número.
     *
     * @param empresaId la empresa
     * @param muestraId la muestra
     * @return las fotos
     */
    List<FotoGuardada> fotos(UUID empresaId, UUID muestraId);

    /**
     * Una foto de una muestra.
     *
     * @param empresaId la empresa
     * @param muestraId la muestra
     * @param numero el número de la foto
     * @return la foto, o vacío si ese número no tiene foto
     */
    Optional<FotoGuardada> foto(UUID empresaId, UUID muestraId, int numero);

    /**
     * Borra una foto de la base, en seguida: así otra puede tomar su número en la misma transacción.
     *
     * @param empresaId la empresa
     * @param fotoId la foto
     */
    void borrarFoto(UUID empresaId, UUID fotoId);

    /**
     * Guarda una foto cargada en la muestra.
     *
     * @param empresaId la empresa
     * @param muestraId la muestra
     * @param numero el número de la foto, de 1 a 3
     * @param llave dónde está en el almacén
     * @param formato su formato
     * @param bytes cuánto pesa
     * @return la foto guardada
     */
    FotoGuardada guardarFoto(UUID empresaId, UUID muestraId, int numero, String llave, FormatoDeFoto formato,
            long bytes);

    /**
     * El avance de la muestra en curso del lote: la de número más alto (RNF-01 C4, RNF-02 C3).
     *
     * @param empresaId la empresa
     * @param loteId el lote
     * @return el avance, o vacío si el lote todavía no tiene muestra
     */
    Optional<AvanceDelAnalisis> avance(UUID empresaId, UUID loteId);

    /**
     * Envía una muestra pendiente a la cola: queda «en espera», con la fecha de envío y con quien la envía como quien
     * la analizó. Si ya se había enviado antes y volvió sin resultado, las fechas guardan este último envío.
     *
     * @param empresaId la empresa
     * @param muestraId la muestra
     * @param usuarioId quien la envía («Analizada por»)
     * @param fechaEnvio cuándo, al microsegundo
     */
    void enviar(UUID empresaId, UUID muestraId, UUID usuarioId, Instant fechaEnvio);

    /**
     * Termina una muestra con su resultado, solo si sigue «en proceso» con la hora de inicio con que se tomó: si otra
     * ejecución ya la tomó o la devolvió, no cambia nada.
     *
     * @param muestra la muestra tomada de la cola
     * @param termino su modelo, sus conteos, su aviso, la copia de los parámetros y su fecha de fin
     * @return {@code true} si la terminó
     */
    boolean terminar(MuestraTomada muestra, TerminoDeMuestra termino);

    /**
     * Devuelve una muestra a pendiente, sin resultado, solo si sigue «en proceso» con la hora de inicio con que se
     * tomó. Conserva su fecha de envío: así la página sabe que el envío volvió sin resultado.
     *
     * @param muestra la muestra tomada de la cola
     * @return {@code true} si la devolvió
     */
    boolean devolver(MuestraTomada muestra);

    /**
     * Guarda los granos de las fotos de una muestra, en ese orden y en lotes de inserción.
     *
     * @param empresaId la empresa de la muestra
     * @param granos los granos, cada uno con su foto
     */
    void guardarGranos(UUID empresaId, List<GranoDeFoto> granos);

    /**
     * Deja aprobadas las fotos de una muestra: las que pasaron al análisis (RF-06).
     *
     * @param empresaId la empresa de la muestra
     * @param muestraId la muestra
     */
    void aprobarFotos(UUID empresaId, UUID muestraId);

    /**
     * Los conteos de las muestras terminadas de un lote, por número, con los recall del modelo de cada una (D-39).
     *
     * @param empresaId la empresa
     * @param loteId el lote
     * @return los conteos: ninguno, uno o dos (D-25)
     */
    List<ConteoDeMuestra> conteosTerminados(UUID empresaId, UUID loteId);

    /**
     * Las muestras de un lote, por número, con quién las analizó y su modelo.
     *
     * @param empresaId la empresa
     * @param loteId el lote
     * @return las muestras: ninguna, una o dos (D-25)
     */
    List<MuestraGuardada> delLote(UUID empresaId, UUID loteId);

    /**
     * Los granos de unas fotos, en el orden en que se guardaron.
     *
     * @param empresaId la empresa
     * @param fotoIds las fotos
     * @return los granos
     */
    List<GranoDeFoto> granos(UUID empresaId, Collection<UUID> fotoIds);
}
