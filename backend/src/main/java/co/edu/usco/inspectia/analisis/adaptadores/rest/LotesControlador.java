/*
 * Nombre: LotesControlador.java
 * Descripción: API REST de los lotes: registrar un lote, el historial por páginas, el detalle y las
 *              sugerencias de finca o proveedor (RF-04, RF-14, RNF-03).
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.ConsultarLotes;
import co.edu.usco.inspectia.analisis.aplicacion.RegistrarLote;
import co.edu.usco.inspectia.comun.api.Pagina;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de los lotes en {@value #RUTA} (RF-04, RF-14). Intercambia JSON. La empresa y el usuario salen
 * de la sesión: un lote de otra empresa responde 404, como si no existiera (RNF-03). Los permisos los
 * revisan la regla por ruta ({@link LotesAcceso}) y {@code @PreAuthorize} (RNF-04).
 */
@RestController
@RequestMapping(LotesControlador.RUTA)
@Tag(name = "Lotes", description = "El usuario final registra los lotes; él y el administrador de empresa "
        + "consultan el historial de la empresa (RF-04, RF-14).")
public class LotesControlador {

    /** Ruta de los lotes. */
    public static final String RUTA = "/api/v1/lotes";

    private final RegistrarLote registrar;
    private final ConsultarLotes consultar;

    /**
     * Crea el controlador; Spring le pasa los casos de uso.
     *
     * @param registrar registrar un lote
     * @param consultar consultar los lotes de la empresa
     */
    public LotesControlador(RegistrarLote registrar, ConsultarLotes consultar) {
        this.registrar = registrar;
        this.consultar = consultar;
    }

    /**
     * Registra un lote, pendiente de análisis. Responde 201 con el lote.
     *
     * @param solicitud los datos del lote
     * @param sesion el usuario final
     * @return el lote registrado
     */
    @PostMapping
    @PreAuthorize("hasRole('USUARIO_FINAL')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un lote", description = "Registra el lote en la empresa de la sesión, pendiente "
            + "de análisis. El código no se puede repetir en la empresa, sin distinguir mayúsculas ni espacios al "
            + "inicio o al final (409), y la fecha de recepción no puede ser posterior a hoy en Colombia ni de hace más "
            + "de un año (400). "
            + "Roles: USUARIO_FINAL.")
    public LoteRespuesta registrar(@Valid @RequestBody LoteSolicitud solicitud,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return LoteRespuesta.de(registrar.registrar(solicitud.datos(), sesion.empresaId(), sesion.id()));
    }

    /**
     * El historial de lotes de la empresa, por páginas.
     *
     * @param pagina número de la página, desde 0
     * @param tamano lotes por página
     * @param sesion el usuario de la sesión: solo ve los de su empresa
     * @return la página
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USUARIO_FINAL', 'ADMIN_EMPRESA')")
    @Operation(summary = "Consultar el historial de lotes", description = "Página de los lotes de la empresa, del "
            + "último movimiento al más antiguo: la fecha del resultado si el lote ya se analizó, o la de registro si "
            + "está pendiente. Roles: USUARIO_FINAL y ADMIN_EMPRESA.")
    public Pagina<LoteRespuesta> historial(
            @Parameter(description = "Número de la página, de 0 a " + Pagina.PAGINA_MAXIMA + ".")
            @RequestParam(name = "pagina", defaultValue = "0")
            @Min(value = 0, message = "{validacion.minimo}")
            @Max(value = Pagina.PAGINA_MAXIMA, message = "{validacion.maximo}") int pagina,
            @Parameter(description = "Lotes por página, de 1 a " + Pagina.TAMANO_MAXIMO + ".")
            @RequestParam(name = "tamano", defaultValue = "" + Pagina.TAMANO_POR_DEFECTO)
            @Min(value = 1, message = "{validacion.minimo}")
            @Max(value = Pagina.TAMANO_MAXIMO, message = "{validacion.maximo}") int tamano,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return Pagina.de(consultar.historial(sesion.empresaId(), pagina, tamano), LoteRespuesta::de);
    }

    /**
     * Las fincas y los proveedores que ya usó la empresa, para sugerirlos al registrar un lote.
     *
     * @param texto lo que el usuario lleva escrito
     * @param sesion el usuario de la sesión: solo ve los de su empresa
     * @return las sugerencias
     */
    @GetMapping("/origenes")
    @PreAuthorize("hasAnyRole('USUARIO_FINAL', 'ADMIN_EMPRESA')")
    @Operation(summary = "Sugerir fincas o proveedores", description = "Las fincas y los proveedores de la empresa "
            + "que contienen el texto, sin distinguir mayúsculas, tildes ni los espacios del inicio y del final. Cada "
            + "uno aparece una vez, con la forma más usada y, si empatan, la más reciente; en orden alfabético y como "
            + "máximo 10 (RF-04). Roles: USUARIO_FINAL y ADMIN_EMPRESA.")
    public List<String> origenes(
            @Parameter(description = "Lo que el usuario lleva escrito; vacío para no filtrar.")
            @RequestParam(name = "texto", defaultValue = "")
            @Size(max = 120, message = "{validacion.longitud_max}") String texto,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return consultar.origenes(sesion.empresaId(), texto);
    }

    /**
     * El detalle de un lote de la empresa.
     *
     * @param id el lote
     * @param sesion el usuario de la sesión
     * @return el lote
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USUARIO_FINAL', 'ADMIN_EMPRESA')")
    @Operation(summary = "Ver un lote", description = "El lote, con su resultado si ya se analizó. Si no existe o es "
            + "de otra empresa, responde 404 (RNF-03). Roles: USUARIO_FINAL y ADMIN_EMPRESA.")
    public LoteRespuesta ver(@PathVariable("id") UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return LoteRespuesta.de(consultar.detalle(sesion.empresaId(), id));
    }
}
