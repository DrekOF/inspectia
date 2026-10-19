/*
 * Nombre: ManejadorDeErrores.java
 * Descripción: Convierte los errores de la API en respuestas application/problem+json (RFC 9457), con
 *              la lista «errores» y los mensajes en el idioma de la petición (RP-03, RP-11).
 * Fecha de creación: 13/10/2026
 * Actualización: 19/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.4
 */
package co.edu.usco.inspectia.comun.errores;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Convierte los errores de la API en respuestas {@code application/problem+json} (RFC 9457). Las de
 * datos inválidos (400) y repetidos (409) traen la lista {@code errores}, con un {@link ErrorDeCampo}
 * por cada campo que corregir. Los mensajes salen en el idioma de la petición ({@code Accept-Language});
 * si no pide ninguno, en español (RP-11).
 *
 * <p>Los 401 y 403 no pasan por aquí: los responde la cadena de seguridad
 * ({@code co.edu.usco.inspectia.comun.seguridad.RespuestasDeSeguridad}). La excepción es el 401 de un
 * inicio de sesión rechazado ({@link SesionRechazadaExcepcion}), que sale del servicio de la sesión.
 */
@Slf4j
@RestControllerAdvice
public class ManejadorDeErrores extends ResponseEntityExceptionHandler {

    /** Código de PostgreSQL de una violación de un índice único. */
    private static final String VIOLACION_DE_UNICO = "23505";

    /**
     * Si un campo falla varias validaciones, se informa primero que falta. Un dato opcional que llega en
     * blanco ({@code @NoEnBlanco}) cuenta como uno que falta: responde lo mismo que {@code @NotBlank}.
     */
    private static final List<String> FALTA_EL_DATO = List.of("NotNull", "NotBlank", "NotEmpty", "NoEnBlanco");

    /** Código del error de {@code @ValorPermitido}: un valor de una lista que no se puede asignar. */
    private static final String VALOR_NO_PERMITIDO = "ValorPermitido";

    @ExceptionHandler(NoEncontradoExcepcion.class)
    ProblemDetail noEncontrado(NoEncontradoExcepcion excepcion) {
        return problema(HttpStatus.NOT_FOUND, "error.no_encontrado", List.of());
    }

    @ExceptionHandler(DatoRepetidoExcepcion.class)
    ProblemDetail datoRepetido(DatoRepetidoExcepcion excepcion) {
        return problema(HttpStatus.CONFLICT, "error.dato_repetido", traducir(excepcion.getCampos()));
    }

    @ExceptionHandler(ReglaIncumplidaExcepcion.class)
    ProblemDetail reglaIncumplida(ReglaIncumplidaExcepcion excepcion) {
        return problema(HttpStatus.BAD_REQUEST, "error.datos_invalidos", traducir(excepcion.getCampos()));
    }

    /** Inicio de sesión rechazado (RF-01): 401 con el motivo y, si lleva, la ayuda en {@code ayuda}. */
    @ExceptionHandler(SesionRechazadaExcepcion.class)
    ProblemDetail sesionRechazada(SesionRechazadaExcepcion excepcion) {
        ProblemDetail problema = problema(HttpStatus.UNAUTHORIZED, excepcion.getClave(), List.of());
        if (excepcion.getAyuda() != null) {
            problema.setProperty("ayuda", mensaje(excepcion.getAyuda(), excepcion.getArgumentosAyuda()));
        }
        return problema;
    }

    /**
     * Al cambiar la propia contraseña, la actual no es la correcta (RF-18): 400 con el error en su campo
     * y, en {@code ayuda}, la regla del bloqueo, como en el inicio de sesión.
     */
    @ExceptionHandler(ContrasenaActualIncorrectaExcepcion.class)
    ProblemDetail contrasenaActualIncorrecta(ContrasenaActualIncorrectaExcepcion excepcion) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "error.datos_invalidos", traducir(excepcion.getCampos()));
        problema.setProperty("ayuda", mensaje("error.ayuda_bloqueo", excepcion.getIntentosPermitidos(),
                excepcion.getMinutosDeBloqueo()));
        return problema;
    }

    /**
     * La cuenta está bloqueada por intentos fallidos (RF-18 C2): 429 con la hora en que termina el
     * bloqueo en {@code bloqueadoHasta} y los segundos que faltan en el encabezado {@code Retry-After}.
     */
    @ExceptionHandler(CuentaBloqueadaExcepcion.class)
    ResponseEntity<ProblemDetail> cuentaBloqueada(CuentaBloqueadaExcepcion excepcion) {
        ProblemDetail problema = problema(HttpStatus.TOO_MANY_REQUESTS, "error.cuenta_bloqueada", List.of());
        problema.setProperty("bloqueadoHasta", excepcion.getBloqueadoHasta());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(excepcion.getSegundosRestantes()))
                .body(problema);
    }

    /**
     * La base rechazó un dato. Si fue un índice único, es una carrera: dos peticiones guardaron el
     * mismo valor a la vez, después de que el servicio revisó que no existía (409). Lo demás es un
     * error del servidor: los CHECK de la base son la última defensa, no la validación.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException excepcion) {
        if (excepcion.getMostSpecificCause() instanceof SQLException sql
                && VIOLACION_DE_UNICO.equals(sql.getSQLState())) {
            return problema(HttpStatus.CONFLICT, "error.dato_repetido", List.of());
        }
        return errorInterno(excepcion);
    }

    /**
     * Cualquier otro error: se registra en el servidor y el usuario ve un mensaje general (500). Los
     * de seguridad siguen su camino hasta la cadena de seguridad, que responde 401 o 403.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception excepcion) throws Exception {
        if (excepcion instanceof AccessDeniedException || excepcion instanceof AuthenticationException) {
            throw excepcion;
        }
        return errorInterno(excepcion);
    }

    /**
     * Datos inválidos: 400 con un error por campo. Si además algún valor de una lista no se puede
     * asignar ({@code @ValorPermitido}), responde 403 solo con esos: es un tema de permisos y se
     * revisa antes que los datos, como las reglas por ruta (RNF-04, RF-03 C10).
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException excepcion,
            HttpHeaders encabezados, HttpStatusCode estado, WebRequest peticion) {
        List<ErrorDeCampo> noPermitidos = excepcion.getBindingResult().getFieldErrors().stream()
                .filter(error -> VALOR_NO_PERMITIDO.equals(error.getCode()))
                .map(error -> new ErrorDeCampo(error.getField(), error.getDefaultMessage()))
                .toList();
        if (!noPermitidos.isEmpty()) {
            ProblemDetail problema = problema(HttpStatus.FORBIDDEN, "error.prohibido", noPermitidos);
            return handleExceptionInternal(excepcion, problema, encabezados, HttpStatus.FORBIDDEN, peticion);
        }
        ProblemDetail problema = problema(estado, "error.datos_invalidos", errores(excepcion.getBindingResult()));
        return handleExceptionInternal(excepcion, problema, encabezados, estado, peticion);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException excepcion, HttpHeaders encabezados, HttpStatusCode estado,
            WebRequest peticion) {
        List<ErrorDeCampo> errores = excepcion.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream().limit(1)
                        .map(error -> new ErrorDeCampo(resultado.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        ProblemDetail problema = problema(estado, "error.datos_invalidos", errores);
        return handleExceptionInternal(excepcion, problema, encabezados, estado, peticion);
    }

    /**
     * El cuerpo no se puede leer: 400. Si el JSON es válido pero un campo trae un valor que no es de su
     * tipo, como un código que no existe en una lista («JEFE» como rol), dice cuál campo.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException excepcion,
            HttpHeaders encabezados, HttpStatusCode estado, WebRequest peticion) {
        String campo = campoConValorInvalido(excepcion);
        ProblemDetail problema = campo == null
                ? problema(estado, "error.cuerpo_invalido", List.of())
                : problema(estado, "error.datos_invalidos",
                        List.of(new ErrorDeCampo(campo, mensaje("error.valor_invalido"))));
        return handleExceptionInternal(excepcion, problema, encabezados, estado, peticion);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException excepcion, HttpHeaders encabezados,
            HttpStatusCode estado, WebRequest peticion) {
        String campo = excepcion instanceof MethodArgumentTypeMismatchException argumento
                ? argumento.getName() : excepcion.getPropertyName();
        ProblemDetail problema = problema(estado, "error.datos_invalidos",
                List.of(new ErrorDeCampo(campo, mensaje("error.valor_invalido"))));
        return handleExceptionInternal(excepcion, problema, encabezados, estado, peticion);
    }

    /** El campo, como {@code rol} o {@code administrador.correo}, cuyo valor no es de su tipo; o {@code null}. */
    private static String campoConValorInvalido(HttpMessageNotReadableException excepcion) {
        for (Throwable causa = excepcion; causa != null; causa = causa.getCause()) {
            if (causa instanceof InvalidFormatException formato) {
                String campo = formato.getPath().stream()
                        .map(JacksonException.Reference::getPropertyName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("."));
                return campo.isEmpty() ? null : campo;
            }
        }
        return null;
    }

    private ProblemDetail errorInterno(Exception excepcion) {
        log.error("Error inesperado en la API", excepcion);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "error.interno", List.of());
    }

    private ProblemDetail problema(HttpStatusCode estado, String clave, List<ErrorDeCampo> errores) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, mensaje(clave));
        if (!errores.isEmpty()) {
            problema.setProperty("errores", errores);
        }
        return problema;
    }

    /** Un error por campo, en orden alfabético de campo; los errores que no son de un campo, al final. */
    private List<ErrorDeCampo> errores(BindingResult resultado) {
        Map<String, FieldError> porCampo = new TreeMap<>();
        resultado.getFieldErrors().stream()
                .sorted(Comparator.comparing((FieldError error) -> !FALTA_EL_DATO.contains(error.getCode()))
                        .thenComparing(error -> String.valueOf(error.getCode())))
                .forEach(error -> porCampo.putIfAbsent(error.getField(), error));
        List<ErrorDeCampo> errores = new ArrayList<>();
        porCampo.forEach((campo, error) -> errores.add(new ErrorDeCampo(campo, error.getDefaultMessage())));
        resultado.getGlobalErrors().forEach(error -> errores.add(new ErrorDeCampo(null, error.getDefaultMessage())));
        return errores;
    }

    private List<ErrorDeCampo> traducir(List<CampoInvalido> campos) {
        return campos.stream().map(campo -> new ErrorDeCampo(campo.campo(), mensaje(campo.clave()))).toList();
    }

    private String mensaje(String clave, Object... argumentos) {
        MessageSource mensajes = getMessageSource();
        Object[] valores = argumentos.length == 0 ? null : argumentos;
        return mensajes == null ? clave : mensajes.getMessage(clave, valores, clave, LocaleContextHolder.getLocale());
    }
}
