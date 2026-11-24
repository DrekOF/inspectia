/*
 * Nombre: ConfirmarFoto.java
 * Descripción: Caso de uso que confirma una foto subida y le pide al servicio de IA su revisión rápida fuera de toda
 *              transacción: la foto queda «Aprobada» o «Por repetir» con su motivo o, si el servicio no responde,
 *              «Cargada» (RF-05, RF-06, RF-07, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Confirmar una foto (RF-05) con su revisión rápida (D-46), en dos transacciones cortas y la revisión entre ellas:
 *
 * <ol>
 *   <li>{@link SubirFotos#confirmar} la guarda «Cargada», con todas sus reglas, como siempre.</li>
 *   <li>Si el revisor revisa (con el servicio de IA), se lee la foto del almacén y se le pide la revisión, sin nada
 *       abierto en la base: puede tardar hasta 10 s. Si no responde o falla, la foto queda «Cargada» y la revisa la
 *       cola antes de clasificarla.</li>
 *   <li>{@link GuardarRevisionDeFoto} guarda la revisión, si la foto sigue ahí y cargada, y responde la foto con su
 *       estado nuevo.</li>
 * </ol>
 *
 * <p>Con el clasificador falso no hay revisión: la foto queda «Cargada», como antes. Confirmar dos veces la misma llave
 * responde la misma foto, ya revisada, sin pedir otra revisión.
 */
@Slf4j
@Service
public class ConfirmarFoto {

    private final SubirFotos subir;
    private final RevisorDeFotos revisor;
    private final AlmacenDeArchivos almacen;
    private final GuardarRevisionDeFoto revisiones;

    /**
     * Crea el caso de uso; Spring le pasa sus dependencias.
     *
     * @param subir la confirmación de la foto, que la guarda cargada
     * @param revisor la revisión rápida de las fotos
     * @param almacen el almacén de los archivos, de donde se lee la foto
     * @param revisiones la transacción que guarda la revisión
     */
    public ConfirmarFoto(SubirFotos subir, RevisorDeFotos revisor, AlmacenDeArchivos almacen,
            GuardarRevisionDeFoto revisiones) {
        this.subir = subir;
        this.revisor = revisor;
        this.almacen = almacen;
        this.revisiones = revisiones;
    }

    /**
     * Confirma la foto {@code numero} y la revisa.
     *
     * @param empresaId la empresa de la sesión
     * @param usuarioId el usuario de la sesión
     * @param loteId el lote
     * @param numero el número de la foto, de 1 a 3
     * @param llave la llave que dio el enlace de subida
     * @return la foto con su estado: aprobada, por repetir o, sin revisión, cargada
     * @see SubirFotos#confirmar para los errores, que no cambian
     */
    public VistaDeFoto confirmar(UUID empresaId, UUID usuarioId, UUID loteId, int numero, String llave) {
        VistaDeFoto confirmada = subir.confirmar(empresaId, usuarioId, loteId, numero, llave);
        if (!revisor.revisa()) {
            return confirmada;
        }
        if (confirmada.estado() != EstadoDeFoto.CARGADA) {
            // La misma llave confirmada otra vez: ya tiene su revisión.
            return revisiones.vista(empresaId, loteId, numero, llave).orElse(confirmada);
        }
        RevisionDeFoto revision;
        try {
            byte[] contenido = almacen.leer(llave);
            revision = revisor.revisar(new FotoParaClasificar(numero, confirmada.formato(), contenido));
        } catch (RuntimeException excepcion) {
            log.warn("No se pudo revisar la foto {} del lote {}: queda cargada y la revisa la cola", numero, loteId,
                    excepcion);
            return confirmada;
        }
        return revisiones.guardar(empresaId, loteId, numero, llave, revision).orElse(confirmada);
    }
}
