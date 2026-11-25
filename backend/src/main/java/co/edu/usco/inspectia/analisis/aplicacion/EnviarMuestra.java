/*
 * Nombre: EnviarMuestra.java
 * Descripción: Caso de uso de «Analizar la muestra»: envía a la cola la muestra en curso de un lote, con 2 o 3 fotos, y
 *              despierta a la cola cuando se confirma; también la muestra 2 de un lote analizado que sugiere otra
 *              muestra; no la envía con fotos por repetir (RF-05 C2, RF-06, RF-07, RF-09, RF-12 C8, RF-14 C6,
 *              RNF-02 C3, D-25, D-44).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.ReglasDeFotos;
import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.ConflictoExcepcion;
import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * «Analizar la muestra» (RF-05 C2, RNF-02 C3). La empresa y el usuario salen de la sesión (RNF-03). Bloquea el lote
 * primero, como {@link SubirFotos#confirmar}: así no se cruza con una foto que se confirma al mismo tiempo.
 *
 * <ul>
 *   <li>Sin muestra, o con una pendiente de menos de 2 fotos: 400 en {@code fotos}, sin cambiar nada (RF-05 C2).</li>
 *   <li>Con una pendiente con alguna foto «Por repetir»: 400 en {@code fotos}, sin cambiar nada (RF-06, RF-07, RF-09):
 *       primero hay que repetirla.</li>
 *   <li>Con una pendiente de 2 o 3 fotos: queda «en espera», con la fecha de envío y con quien la envía como quien la
 *       analizó, y sin el motivo de una devolución anterior (D-44). Cuando la transacción se confirma, el evento
 *       {@link MuestraEnviada} despierta a la cola.</li>
 *   <li>Ya «en espera» o «en proceso»: responde su avance sin cambiar nada, así un reintento es seguro.</li>
 *   <li>Un lote analizado que sugiere otra muestra (D-24) solo envía su muestra 2, con las mismas reglas: sin la 2, o
 *       con menos de 2 fotos en ella, 400 en {@code fotos} (D-25). El lote sigue analizado con el resultado de la 1
 *       hasta que la 2 termina.</li>
 *   <li>Un lote analizado sin sugerencia, o con sus 2 muestras terminadas, no se vuelve a analizar: 409
 *       {@code analisis.lote_analizado} (RF-12 C8).</li>
 * </ul>
 */
@Service
public class EnviarMuestra {

    private final RepositorioDeLotes lotes;
    private final RepositorioDeMuestras muestras;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    /**
     * Crea el caso de uso; Spring le pasa sus dependencias.
     *
     * @param lotes los lotes guardados
     * @param muestras las muestras y sus fotos
     * @param eventos para despertar a la cola después de confirmar
     * @param reloj el reloj de la aplicación, en UTC
     */
    public EnviarMuestra(RepositorioDeLotes lotes, RepositorioDeMuestras muestras, ApplicationEventPublisher eventos,
            Clock reloj) {
        this.lotes = lotes;
        this.muestras = muestras;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /**
     * Envía a analizar la muestra en curso del lote: la 1, o la 2 de un lote que sugiere otra muestra.
     *
     * @param empresaId la empresa de la sesión
     * @param usuarioId el usuario de la sesión, que queda como quien analizó la muestra
     * @param loteId el lote
     * @return el avance de la muestra: «en espera», o el que ya tenía si se había enviado
     * @throws NoEncontradoExcepcion si el lote no existe o es de otra empresa (404, RNF-03 C1)
     * @throws ConflictoExcepcion si el lote ya tiene su resultado y no admite otra muestra (409, RF-12 C8)
     * @throws ReglaIncumplidaExcepcion si la muestra no tiene al menos 2 fotos, si el lote sugiere otra muestra y la 2
     *     todavía no existe (400 en {@code fotos}, RF-05 C2), o si tiene alguna foto por repetir (400 en {@code fotos})
     */
    @Transactional
    public AvanceDelAnalisis enviar(UUID empresaId, UUID usuarioId, UUID loteId) {
        EstadoDeLote estado = lotes.estadoParaActualizar(empresaId, loteId)
                .orElseThrow(() -> new NoEncontradoExcepcion("lote " + loteId));
        boolean analizado = estado != EstadoDeLote.PENDIENTE;
        if (analizado && !lotes.sugiereOtraMuestra(empresaId, loteId)) {
            throw new ConflictoExcepcion("analisis.lote_analizado");
        }
        MuestraEnCurso muestra = muestras.muestraEnCurso(empresaId, loteId).orElseThrow(EnviarMuestra::faltanFotos);
        if (analizado && muestra.numero() == 1) {
            // La muestra en curso sigue siendo la 1, ya terminada: la 2 todavía no tiene fotos (D-25).
            throw faltanFotos();
        }
        if (muestra.estado() == EstadoDeMuestra.EN_ESPERA || muestra.estado() == EstadoDeMuestra.EN_PROCESO) {
            return muestras.avance(empresaId, loteId).orElseThrow();
        }
        if (muestra.estado() != EstadoDeMuestra.PENDIENTE) {
            throw new ConflictoExcepcion("analisis.lote_analizado");
        }
        List<FotoGuardada> fotos = muestras.fotos(empresaId, muestra.id());
        if (!ReglasDeFotos.alcanzanParaAnalizar(fotos.size())) {
            throw faltanFotos();
        }
        if (fotos.stream().anyMatch(foto -> foto.estado() == EstadoDeFoto.POR_REPETIR)) {
            throw new ReglaIncumplidaExcepcion(List.of(new CampoInvalido("fotos", "analisis.fotos.por_repetir")));
        }
        // PostgreSQL guarda microsegundos: así la fecha que se responde es la misma que queda guardada.
        Instant ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
        muestras.enviar(empresaId, muestra.id(), usuarioId, ahora);
        eventos.publishEvent(new MuestraEnviada(muestra.id()));
        return AvanceDelAnalisis.de(muestra.numero(), EstadoDeMuestra.EN_ESPERA, ahora, null, null);
    }

    /** La muestra necesita al menos 2 fotos (RF-05 C2); el mensaje dice qué hacer (RNF-06 C3). */
    private static ReglaIncumplidaExcepcion faltanFotos() {
        return new ReglaIncumplidaExcepcion(List.of(new CampoInvalido("fotos", "analisis.fotos.minimo")));
    }
}
