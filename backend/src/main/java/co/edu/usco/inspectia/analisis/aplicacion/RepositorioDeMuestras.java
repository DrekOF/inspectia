/*
 * Nombre: RepositorioDeMuestras.java
 * Descripción: Puerto de las muestras de los lotes y de sus fotos (D-21, RF-05). Todas las consultas reciben la
 *              empresa de la sesión (RNF-03).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de las muestras y sus fotos (D-21). Todas las consultas reciben la empresa de la sesión y solo responden
 * datos de ella (RNF-03). Lo implementa el adaptador de JPA.
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
}
