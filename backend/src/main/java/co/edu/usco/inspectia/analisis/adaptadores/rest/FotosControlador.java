/*
 * Nombre: FotosControlador.java
 * Descripción: API REST de las fotos de la muestra de un lote (RF-05, RF-14 C5): listarlas, pedir el enlace para subir
 *              una y confirmarla. Solo el usuario final, con lotes de su empresa (RNF-03, RNF-04).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.ConsultarFotos;
import co.edu.usco.inspectia.analisis.aplicacion.SubirFotos;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de las fotos de la muestra en curso de un lote, en {@value #RUTA} (RF-05, RF-14 C5). La página sigue el
 * mismo flujo que con S3 (D-41): pide el enlace de subida de cada foto, sube el archivo directo al almacén, sin el
 * token, y confirma la foto. Solo el usuario final usa estas rutas (RF-05; RF-14, restricción 4); la empresa sale de
 * la sesión, y un lote de otra empresa responde 404 (RNF-03). Los permisos los revisan la regla por ruta
 * ({@link LotesAcceso}) y {@code @PreAuthorize} (RNF-04).
 */
@RestController
@RequestMapping(FotosControlador.RUTA)
@Tag(name = "Fotos", description = "Las fotos de la muestra de un lote (RF-05, RF-14 C5).")
public class FotosControlador {

    /** Ruta de las fotos de un lote. */
    public static final String RUTA = LotesControlador.RUTA + "/{id}/fotos";

    private final SubirFotos subir;
    private final ConsultarFotos consultar;

    /**
     * Crea el controlador; Spring le pasa los casos de uso.
     *
     * @param subir subir las fotos
     * @param consultar consultar las fotos
     */
    public FotosControlador(SubirFotos subir, ConsultarFotos consultar) {
        this.subir = subir;
        this.consultar = consultar;
    }

    /**
     * Las fotos de la muestra en curso, cada una con un enlace nuevo para verla.
     *
     * @param id el lote
     * @param sesion el usuario de la sesión: solo ve los lotes de su empresa
     * @return la muestra y sus fotos
     */
    @GetMapping
    @PreAuthorize("hasRole('USUARIO_FINAL')")
    @Operation(summary = "Listar las fotos de la muestra", description = "Las fotos de la muestra en curso del lote, "
            + "por número, cada una con un enlace para verla que vence en 15 minutos. Sin fotos, la muestra va vacía. "
            + "Un lote de otra empresa responde 404. Rol: USUARIO_FINAL.")
    public FotosDeLaMuestraRespuesta fotos(@PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return FotosDeLaMuestraRespuesta.de(consultar.fotos(sesion.empresaId(), id));
    }

    /**
     * El enlace para subir la foto {@code numero}.
     *
     * @param id el lote
     * @param numero el número de la foto, de 1 a 3
     * @param solicitud el tipo y el tamaño del archivo
     * @param sesion el usuario de la sesión
     * @return la llave del archivo y el enlace para subirlo
     */
    @PostMapping("/{numero}/subida")
    @PreAuthorize("hasRole('USUARIO_FINAL')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Pedir el enlace para subir una foto", description = "Revisa que el lote esté pendiente "
            + "(409 si no) y que el archivo sea JPG o PNG, de hasta 10 MB (400). Responde la llave del archivo y el "
            + "enlace: se sube con PUT, sin el token y con el Content-Type que indica, y vence en 10 minutos. Después "
            + "se confirma la foto. Rol: USUARIO_FINAL.")
    public SubidaRespuesta pedirSubida(@PathVariable UUID id,
            @PathVariable @Min(value = 1, message = "{validacion.minimo}")
            @Max(value = 3, message = "{validacion.maximo}") int numero,
            @Valid @RequestBody SolicitudDeSubida solicitud,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return SubidaRespuesta.de(subir.pedirSubida(sesion.empresaId(), id, numero, solicitud.tipo(),
                solicitud.tamanoBytes()));
    }

    /**
     * Confirma la foto {@code numero}, ya subida.
     *
     * @param id el lote
     * @param numero el número de la foto, de 1 a 3
     * @param confirmacion la llave que dio el enlace de subida
     * @param sesion el usuario de la sesión
     * @return la foto, con su enlace para verla
     */
    @PutMapping("/{numero}")
    @PreAuthorize("hasRole('USUARIO_FINAL')")
    @Operation(summary = "Confirmar una foto subida", description = "Revisa que el archivo haya llegado completo y que "
            + "sea JPG o PNG por dentro, y guarda la foto, cargada. La primera foto crea la muestra; una foto con el "
            + "número de otra la reemplaza, y la anterior se borra. Confirmar dos veces la misma llave responde la "
            + "misma foto. Rol: USUARIO_FINAL.")
    public FotoRespuesta confirmar(@PathVariable UUID id,
            @PathVariable @Min(value = 1, message = "{validacion.minimo}")
            @Max(value = 3, message = "{validacion.maximo}") int numero,
            @Valid @RequestBody ConfirmacionDeFoto confirmacion,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return FotoRespuesta.de(subir.confirmar(sesion.empresaId(), sesion.id(), id, numero, confirmacion.llave()));
    }
}
