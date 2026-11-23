/*
 * Nombre: ApiLotes.java
 * Descripción: Llamadas a la API de lotes y sus mensajes, para las pruebas (RF-04, RF-14, RNF-03,
 *              RNF-04).
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Llamadas a la API de lotes ({@code /api/v1/lotes}), con el token de quien las hace, y los mensajes que
 * responde.
 *
 * <p>Contrato que fijan estas pruebas (RF-04, RF-14):
 * <ul>
 *   <li>Registrar recibe {@code codigo}, {@code fechaRecepcion} ({@code aaaa-mm-dd}), {@code origen} (la
 *       finca o el proveedor) y {@code observaciones}, opcional. Responde 201 con el lote.</li>
 *   <li>El lote trae {@code id}, {@code codigo}, {@code fechaRecepcion}, {@code origen},
 *       {@code observaciones}, {@code estado} ({@code PENDIENTE} o {@code ANALIZADO}), {@code usuario}
 *       ({@code id} y {@code nombre} de quien lo registró), {@code porcentajeSanos}, {@code categoria},
 *       {@code fechaResultado} y {@code fechaCreacion}. Mientras está pendiente, el resultado va vacío.</li>
 *   <li>El historial responde una página ({@code contenido}, {@code pagina}, {@code tamano},
 *       {@code totalElementos} y {@code totalPaginas}) de 20 lotes, del más reciente al más antiguo.</li>
 *   <li>Las sugerencias de finca o proveedor ({@code /origenes?texto=}) responden una lista de textos.</li>
 *   <li>La empresa y el usuario salen de la sesión: un lote de otra empresa responde 404, como si no
 *       existiera (RNF-03).</li>
 * </ul>
 */
final class ApiLotes {

    static final String RUTA = "/api/v1/lotes";
    static final String ORIGENES = RUTA + "/origenes";

    /**
     * Una fecha de recepción de hace una semana en Colombia, para los lotes que no prueban la fecha. Es relativa a
     * hoy: estas pruebas usan el reloj real, y una fecha fija pasaría a ser de hace más de un año (RF-04).
     */
    static final LocalDate RECEPCION = LocalDate.now(ZoneId.of("America/Bogota")).minusWeeks(1);

    static final String DATOS_INVALIDOS = "Revisa los datos marcados.";
    static final String DATO_REPETIDO = "Ya existe un registro con esos datos.";
    static final String NO_EXISTE = "No existe un registro con ese identificador.";
    static final String OBLIGATORIO = "Este dato es obligatorio.";
    static final String VALOR_INVALIDO = "El valor no es válido.";
    static final String CODIGO_REPETIDO = "Ya existe un lote con este código en la empresa.";
    static final String FECHA_FUTURA = "La fecha de recepción no puede ser posterior a hoy.";
    static final String FECHA_ANTIGUA = "La fecha de recepción no puede ser de hace más de un año.";

    private final ApiSesion api;

    ApiLotes(ApiSesion api) {
        this.api = api;
    }

    MvcTestResult registrar(String token, Map<String, Object> datos) {
        return api.enviar(HttpMethod.POST, RUTA, datos, token);
    }

    MvcTestResult ver(String token, UUID id) {
        return api.enviar(HttpMethod.GET, RUTA + "/" + id, null, token);
    }

    /** Una página del historial, con el tamaño de siempre. */
    MvcTestResult listar(String token, int pagina) {
        return api.enviar(HttpMethod.GET, RUTA + "?pagina=" + pagina, null, token);
    }

    MvcTestResult origenes(String token, String texto) {
        return api.enviar(HttpMethod.GET, ORIGENES + "?texto=" + texto, null, token);
    }

    /** Registra un lote de la Finca La Esperanza y devuelve su id; falla si no responde 201. */
    UUID registrarOk(String token, String codigo) {
        return registrarOk(token, codigo, "Finca La Esperanza");
    }

    /** Registra un lote y devuelve su id; falla si no responde 201. */
    UUID registrarOk(String token, String codigo, String origen) {
        MvcTestResult resultado = registrar(token, datos(codigo, RECEPCION, origen, null));
        assertThat(resultado).as("registrar %s", codigo).hasStatus(HttpStatus.CREATED);
        return UUID.fromString((String) ApiSesion.json(resultado).get("id"));
    }

    /**
     * Los datos de un lote para registrarlo.
     *
     * @param fecha la fecha de recepción; {@code null} para no enviarla
     * @param observaciones las observaciones; {@code null} para no enviarlas
     */
    static Map<String, Object> datos(String codigo, LocalDate fecha, String origen, String observaciones) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", codigo);
        datos.put("fechaRecepcion", fecha == null ? null : fecha.toString());
        datos.put("origen", origen);
        if (observaciones != null) {
            datos.put("observaciones", observaciones);
        }
        return datos;
    }
}
