/*
 * Nombre: GuardarRevisionDeFoto.java
 * Descripción: Caso de uso que guarda, en una transacción corta, la revisión rápida de una foto recién confirmada, y
 *              que responde la vista de una foto con su revisión (RF-06, RF-07, D-45, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda la revisión rápida de una foto (D-46). Es la segunda transacción de confirmar una foto: la primera la guarda
 * «Cargada» ({@link SubirFotos#confirmar}) y, entre las dos, {@link ConfirmarFoto} la revisa sin nada abierto en la base.
 * Bloquea el lote primero, como al confirmar, y solo guarda la revisión si la foto sigue ahí, con la misma llave, en la
 * muestra pendiente que recibe las fotos y todavía «Cargada»: si mientras tanto la reemplazaron o enviaron la muestra,
 * no cambia nada.
 */
@Service
public class GuardarRevisionDeFoto {

    private final RepositorioDeLotes lotes;
    private final RepositorioDeMuestras muestras;
    private final VistasDeFotosRevisadas vistas;

    /**
     * Crea el caso de uso; Spring le pasa sus dependencias.
     *
     * @param lotes los lotes guardados
     * @param muestras las muestras y sus fotos
     * @param vistas las vistas de las fotos con su revisión
     */
    public GuardarRevisionDeFoto(RepositorioDeLotes lotes, RepositorioDeMuestras muestras,
            VistasDeFotosRevisadas vistas) {
        this.lotes = lotes;
        this.muestras = muestras;
        this.vistas = vistas;
    }

    /**
     * Guarda la revisión de la foto {@code numero} si sigue «Cargada» con esa llave.
     *
     * @param empresaId la empresa de la sesión
     * @param loteId el lote
     * @param numero el número de la foto
     * @param llave su llave
     * @param revision lo que dijo el servicio de IA
     * @return la foto con su revisión; vacío si ya no es esa foto o ya no está cargada
     */
    @Transactional
    public Optional<VistaDeFoto> guardar(UUID empresaId, UUID loteId, int numero, String llave,
            RevisionDeFoto revision) {
        if (lotes.estadoParaActualizar(empresaId, loteId).isEmpty()) {
            return Optional.empty();
        }
        Optional<MuestraEnCurso> muestra = muestras.muestraEnCurso(empresaId, loteId)
                .filter(enCurso -> enCurso.estado() == EstadoDeMuestra.PENDIENTE);
        Optional<FotoGuardada> foto = muestra.flatMap(enCurso -> laFoto(empresaId, enCurso, numero, llave))
                .filter(guardada -> guardada.estado() == EstadoDeFoto.CARGADA);
        if (foto.isEmpty()) {
            return Optional.empty();
        }
        muestras.guardarRevision(empresaId, foto.get().id(), revision);
        return vista(empresaId, muestra.get(), numero, llave);
    }

    /**
     * La foto {@code numero} de la muestra en curso con su revisión, si tiene esa llave.
     *
     * @param empresaId la empresa de la sesión
     * @param loteId el lote
     * @param numero el número de la foto
     * @param llave su llave
     * @return la foto; vacío si ya no es esa foto
     */
    @Transactional(readOnly = true)
    public Optional<VistaDeFoto> vista(UUID empresaId, UUID loteId, int numero, String llave) {
        return muestras.muestraEnCurso(empresaId, loteId)
                .flatMap(muestra -> vista(empresaId, muestra, numero, llave));
    }

    private Optional<VistaDeFoto> vista(UUID empresaId, MuestraEnCurso muestra, int numero, String llave) {
        return laFoto(empresaId, muestra, numero, llave)
                .map(foto -> vistas.de(empresaId, muestra, List.of(foto)).get(0));
    }

    private Optional<FotoGuardada> laFoto(UUID empresaId, MuestraEnCurso muestra, int numero, String llave) {
        return muestras.foto(empresaId, muestra.id(), numero).filter(foto -> foto.llave().equals(llave));
    }
}
