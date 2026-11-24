/*
 * Nombre: ConsultarFotos.java
 * Descripción: Caso de uso de consultar las fotos de la muestra en curso de un lote, cada una con un enlace nuevo para
 *              verla, para retomar el lote; con otra muestra sugerida y sin la muestra 2, la lista va vacía (RF-05,
 *              RF-14 C5 y C6, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las fotos de la muestra en curso de un lote (RF-05, RF-14 C5): al retomarlo, la página ve las que ya se subieron.
 * Solo responde lotes de la empresa de la sesión (RNF-03).
 *
 * <p>Un lote analizado que sugiere otra muestra y todavía no tiene la 2 responde la lista vacía y sin número, como un
 * lote nuevo: es la muestra 2 que se va a tomar, y no la 1 ya terminada (RF-14 C6, D-25). Con la 2, la responde a
 * ella; sin la sugerencia, la 1 terminada, como siempre.
 */
@Service
@Transactional(readOnly = true)
public class ConsultarFotos {

    private final RepositorioDeLotes lotes;
    private final RepositorioDeMuestras muestras;
    private final VistasDeFotos vistas;

    /**
     * Crea el caso de uso; Spring le pasa sus dependencias.
     *
     * @param lotes los lotes guardados
     * @param muestras las muestras y sus fotos
     * @param vistas las vistas de las fotos, con su enlace para verlas
     */
    public ConsultarFotos(RepositorioDeLotes lotes, RepositorioDeMuestras muestras, VistasDeFotos vistas) {
        this.lotes = lotes;
        this.muestras = muestras;
        this.vistas = vistas;
    }

    /**
     * Las fotos de la muestra en curso del lote.
     *
     * @param empresaId la empresa de la sesión
     * @param loteId el lote
     * @return la muestra y sus fotos, por número; sin muestra y sin fotos si todavía no se subió ninguna, también la
     *     de la muestra 2
     * @throws NoEncontradoExcepcion si el lote no existe o es de otra empresa (404, RNF-03 C1)
     */
    public FotosDeLaMuestra fotos(UUID empresaId, UUID loteId) {
        EstadoDeLote estado = lotes.estado(empresaId, loteId)
                .orElseThrow(() -> new NoEncontradoExcepcion("lote " + loteId));
        Optional<MuestraEnCurso> enCurso = muestras.muestraEnCurso(empresaId, loteId);
        boolean faltaLaSegunda = estado == EstadoDeLote.ANALIZADO && enCurso.isPresent() && enCurso.get().numero() == 1
                && lotes.sugiereOtraMuestra(empresaId, loteId);
        if (faltaLaSegunda) {
            return new FotosDeLaMuestra(null, null, List.of());
        }
        return enCurso
                .map(muestra -> new FotosDeLaMuestra(muestra.numero(), muestra.estado(),
                        muestras.fotos(empresaId, muestra.id()).stream().map(vistas::de).toList()))
                .orElseGet(() -> new FotosDeLaMuestra(null, null, List.of()));
    }
}
