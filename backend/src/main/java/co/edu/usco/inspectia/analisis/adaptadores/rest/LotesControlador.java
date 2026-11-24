/*
 * Nombre: LotesControlador.java
 * Descripción: API REST de los lotes: registrar un lote, el historial por páginas y con sus filtros, el detalle,
 *              quienes registraron lotes y las sugerencias de finca o proveedor (RF-04, RF-14, RNF-03).
 * Fecha de creación: 10/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.ConsultarLotes;
import co.edu.usco.inspectia.analisis.aplicacion.FiltrosDelHistorial;
import co.edu.usco.inspectia.analisis.aplicacion.RegistrarLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.comun.api.Pagina;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import co.edu.usco.inspectia.rango.RangoRecomendacionCategoria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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
     * El historial de lotes de la empresa, por páginas y con filtros opcionales y combinables (RF-14 C2). Cada filtro
     * es un parámetro suelto y no un objeto: así un valor que no es de su tipo responde 400 en su campo con el mensaje
     * de siempre. Un filtro vacío no filtra.
     *
     * @param pagina número de la página, desde 0
     * @param tamano lotes por página
     * @param desde el primer día de la fecha del resultado, incluido, o vacío
     * @param hasta el último día de la fecha del resultado, incluido, o vacío
     * @param estado pendiente o analizado, o vacío
     * @param categoria la categoría probable, o vacía
     * @param codigo lo que se busca dentro del código del lote, o vacío
     * @param usuario quien registró el lote, o vacío
     * @param sesion el usuario de la sesión: solo ve los de su empresa
     * @return la página
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USUARIO_FINAL', 'ADMIN_EMPRESA')")
    @Operation(summary = "Consultar el historial de lotes", description = "Página de los lotes de la empresa, del "
            + "último movimiento al más antiguo: la fecha del resultado si el lote ya se analizó, o la de registro si "
            + "está pendiente. Los filtros son opcionales y se combinan: el lote cumple todos (RF-14 C2). Las fechas "
            + "filtran la fecha del resultado en días de Colombia, con las dos incluidas: un lote pendiente no tiene "
            + "fecha del resultado y no sale al filtrar por fecha. Si «hasta» es anterior a «desde», responde 400 en "
            + "«hasta». Un valor que no es de su tipo responde 400 en su campo. Filtrar por un usuario de otra empresa "
            + "da la lista vacía (RNF-03). El total y las páginas cuentan solo los lotes que cumplen los filtros. "
            + "Roles: USUARIO_FINAL y ADMIN_EMPRESA.")
    public Pagina<LoteRespuesta> historial(
            @Parameter(description = "Número de la página, de 0 a " + Pagina.PAGINA_MAXIMA + ".")
            @RequestParam(name = "pagina", defaultValue = "0")
            @Min(value = 0, message = "{validacion.minimo}")
            @Max(value = Pagina.PAGINA_MAXIMA, message = "{validacion.maximo}") int pagina,
            @Parameter(description = "Lotes por página, de 1 a " + Pagina.TAMANO_MAXIMO + ".")
            @RequestParam(name = "tamano", defaultValue = "" + Pagina.TAMANO_POR_DEFECTO)
            @Min(value = 1, message = "{validacion.minimo}")
            @Max(value = Pagina.TAMANO_MAXIMO, message = "{validacion.maximo}") int tamano,
            @Parameter(description = "«Resultado desde»: el primer día de la fecha del resultado, en Colombia e "
                    + "incluido, como 2026-11-01.", example = "2026-11-01")
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "«Hasta»: el último día de la fecha del resultado, en Colombia e incluido; no "
                    + "puede ser anterior a «desde».", example = "2026-11-30")
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "El estado del lote: PENDIENTE (de análisis) o ANALIZADO.")
            @RequestParam(name = "estado", required = false) EstadoDeLote estado,
            @Parameter(description = "La categoría probable del lote.")
            @RequestParam(name = "categoria", required = false) RangoRecomendacionCategoria categoria,
            @Parameter(description = "Lo que se busca dentro del código del lote, de hasta 30 caracteres, sin "
                    + "distinguir mayúsculas ni los espacios del inicio y del final; %, _ y \\ se buscan como "
                    + "caracteres.", example = "L-014")
            @RequestParam(name = "codigo", required = false)
            @Size(max = 30, message = "{validacion.longitud_max}") String codigo,
            @Parameter(description = "El identificador de quien registró el lote, de la lista de "
                    + "/api/v1/lotes/registradores.")
            @RequestParam(name = "usuario", required = false) UUID usuario,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        FiltrosDelHistorial filtros = new FiltrosDelHistorial(desde, hasta, estado,
                categoria == null ? null : categoria.name(), codigo, usuario);
        return Pagina.de(consultar.historial(sesion.empresaId(), filtros, pagina, tamano), LoteRespuesta::de);
    }

    /**
     * Quienes registraron lotes de la empresa, para el filtro «Usuario» del historial.
     *
     * @param sesion el usuario de la sesión: solo ve los de su empresa
     * @return su identificador y su nombre
     */
    @GetMapping("/registradores")
    @PreAuthorize("hasAnyRole('USUARIO_FINAL', 'ADMIN_EMPRESA')")
    @Operation(summary = "Listar quienes registraron lotes", description = "El identificador y el nombre de quienes "
            + "registraron lotes de la empresa, una vez cada uno, para el filtro «Usuario» del historial (RF-14 "
            + "restricción 6). Salen también los usuarios desactivados, que siguen en el historial (RF-03 C6), y no "
            + "salen quienes no registraron lotes. No trae correos ni usuarios de otra empresa (RNF-03). Van ordenados "
            + "por nombre, sin distinguir tildes ni mayúsculas. Roles: USUARIO_FINAL y ADMIN_EMPRESA.")
    public List<LoteRespuesta.UsuarioDelLote> registradores(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return consultar.registradores(sesion.empresaId()).stream()
                .map(registrador -> new LoteRespuesta.UsuarioDelLote(registrador.id(), registrador.nombre()))
                .toList();
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
