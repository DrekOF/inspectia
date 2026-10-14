/*
 * Nombre: RegistroEmpresaIT.java
 * Descripción: Pruebas de los criterios de RF-02 sobre el registro de una empresa y su primer
 *              administrador, por la API y con PostgreSQL 18.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.empresa;

import static co.edu.usco.inspectia.empresa.ApiEmpresas.CONTRASENA;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.administrador;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.campos;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.contar;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.errores;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.json;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.registro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import co.edu.usco.inspectia.PruebaIntegracion;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas de RF-02 sobre el registro: la empresa, su primer administrador y sus rangos por defecto se
 * guardan juntos o no se guarda nada. Las pruebas confirman sus transacciones, como en producción, y
 * borran los datos antes y después de cada una.
 */
@PruebaIntegracion
class RegistroEmpresaIT {

    private static final String NIT = "900123456-8";
    private static final String OTRO_NIT = "890903938-8";
    private static final String CORREO = "ana@cacaos.test";

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiEmpresas api;

    @BeforeEach
    void preparar() {
        api = new ApiEmpresas(mvc);
        ApiEmpresas.borrarDatos(jdbc);
    }

    @AfterEach
    void limpiar() {
        ApiEmpresas.borrarDatos(jdbc);
    }

    @Test
    @DisplayName("RF-02 · C1 · con datos válidos, la empresa y su administrador quedan registrados")
    void registraLaEmpresaYSuAdministrador() {
        Map<String, Object> datos = registro(" 900123456-8 ", "  Ana.Perez@Cacaos.TEST ");
        datos.put("nombre", "  Cacaos del Huila  ");
        datos.put("ciudad", " Neiva ");
        administrador(datos).put("nombre", " Ana Pérez ");

        MvcTestResult resultado = api.registrar(datos);

        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        Map<String, Object> respuesta = json(resultado);
        UUID id = UUID.fromString((String) respuesta.get("id"));
        assertThat(resultado.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith(ApiEmpresas.RUTA + "/" + id);
        assertThat(respuesta)
                .containsEntry("nombre", "Cacaos del Huila")
                .containsEntry("nit", NIT)
                .containsEntry("ciudad", "Neiva")
                .containsEntry("activa", true);
        assertThat(jdbc.queryForMap("SELECT nombre, nit, ciudad, activa FROM empresa WHERE id = ?", id))
                .containsEntry("nombre", "Cacaos del Huila")
                .containsEntry("nit", NIT)
                .containsEntry("ciudad", "Neiva")
                .containsEntry("activa", true);
        List<Map<String, Object>> usuarios = jdbc.queryForList(
                "SELECT nombre, correo, rol, activo FROM usuario WHERE empresa_id = ?", id);
        assertThat(usuarios).hasSize(1);
        assertThat(usuarios.getFirst())
                .containsEntry("nombre", "Ana Pérez")
                .containsEntry("correo", "ana.perez@cacaos.test")
                .containsEntry("rol", "ADMIN_EMPRESA")
                .containsEntry("activo", true);
    }

    @Test
    @DisplayName("RF-02 · C2 · si el NIT ya existe, no registra la empresa y lo avisa")
    void noRegistraUnNitRepetido() {
        api.registrarValida(registro(NIT, CORREO));

        MvcTestResult resultado = api.registrar(registro(NIT, "luis@cacaos.test"));

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(errores(resultado)).containsExactly(tuple("nit", "Ya existe una empresa con este NIT."));
        assertThat(contar(jdbc, "empresa")).isEqualTo(1);
        assertThat(contar(jdbc, "usuario")).isEqualTo(1);
        assertThat(contar(jdbc, "rango_recomendacion")).isEqualTo(4);
    }

    @Test
    @DisplayName("RF-02 · C2 · si el correo del administrador ya existe, aunque cambien las mayúsculas, no registra la empresa y lo avisa")
    void noRegistraUnCorreoRepetido() {
        api.registrarValida(registro(NIT, CORREO));

        MvcTestResult resultado = api.registrar(registro(OTRO_NIT, "  ANA@Cacaos.test "));

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(errores(resultado))
                .containsExactly(tuple("administrador.correo", "Ya existe un usuario con este correo."));
        assertThat(contar(jdbc, "empresa")).isEqualTo(1);
        assertThat(contar(jdbc, "usuario")).isEqualTo(1);
        assertThat(contar(jdbc, "rango_recomendacion")).isEqualTo(4);
    }

    @Test
    @DisplayName("RF-02 · C3 · si faltan datos obligatorios, responde un error por cada campo y no guarda nada")
    void marcaCadaDatoQueFalta() {
        Map<String, Object> administrador = new LinkedHashMap<>();
        administrador.put("correo", "  ");
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "   ");
        datos.put("ciudad", "");
        datos.put("administrador", administrador);

        MvcTestResult resultado = api.registrar(datos);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(campos(resultado)).containsExactlyInAnyOrder("nombre", "nit", "ciudad",
                "administrador.nombre", "administrador.correo", "administrador.contrasena");
        assertThat(contar(jdbc, "empresa")).isZero();
        assertThat(contar(jdbc, "usuario")).isZero();
    }

    @Test
    @DisplayName("RF-02 · C3 · si falta el administrador, lo marca y no guarda nada")
    void marcaElAdministradorQueFalta() {
        Map<String, Object> datos = registro(NIT, CORREO);
        datos.remove("administrador");

        MvcTestResult resultado = api.registrar(datos);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(campos(resultado)).containsExactly("administrador");
        assertThat(contar(jdbc, "empresa")).isZero();
    }

    @Test
    @DisplayName("RF-02 · C4 · si el dígito de verificación del NIT no corresponde, lo marca y no registra la empresa")
    void rechazaUnDigitoDeVerificacionInvalido() {
        MvcTestResult resultado = api.registrar(registro("900123456-7", CORREO));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(errores(resultado))
                .containsExactly(tuple("nit", "El dígito de verificación del NIT no corresponde."));
        assertThat(contar(jdbc, "empresa")).isZero();
        assertThat(contar(jdbc, "usuario")).isZero();
    }

    @ParameterizedTest(name = "{0} caracteres")
    @DisplayName("RF-02 · C5 · la contraseña inicial acepta de 8 a 64 caracteres (prueba de límite)")
    @CsvSource(delimiter = '|', textBlock = """
            7  | La contraseña debe tener al menos 8 caracteres.
            8  |
            64 |
            65 | La contraseña puede tener como máximo 64 caracteres.
            """)
    void limiteDeCaracteresDeLaContrasena(int caracteres, String error) {
        comprobarContrasena("Ab1-".repeat(17).substring(0, caracteres), error);
    }

    @ParameterizedTest(name = "{0} eñes en 64 caracteres")
    @DisplayName("RF-02 · C6 · la contraseña inicial ocupa como máximo 72 bytes: con 8 eñes se acepta y con 9 se rechaza con el mensaje de la ficha (prueba de límite)")
    @CsvSource(delimiter = '|', textBlock = """
            8 |
            9 | La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes
            """)
    void limiteDeBytesDeLaContrasena(int enes, String error) {
        comprobarContrasena("ñ".repeat(enes) + "a".repeat(64 - enes), error);
    }

    @Test
    @DisplayName("RF-02 · C7 · la empresa nueva queda con los rangos por defecto de D-10")
    void creaLosRangosPorDefecto() {
        UUID empresa = api.registrarValida(registro(NIT, CORREO));

        List<Map<String, Object>> rangos = jdbc.queryForList("""
                SELECT categoria, limite_inferior::text AS limite, recomendacion FROM rango_recomendacion
                WHERE empresa_id = ? ORDER BY limite_inferior DESC""", empresa);

        assertThat(rangos)
                .extracting(r -> r.get("categoria"), r -> r.get("limite"), r -> r.get("recomendacion"))
                .containsExactly(
                        tuple("PREMIUM", "96.0", "Apto para mercado especial o de exportación."),
                        tuple("ESTANDAR", "90.0",
                                "Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de categoría."),
                        tuple("CORRIENTE", "82.0",
                                "Retirar los granos defectuosos antes de vender, y revisar la fermentación y el secado."),
                        tuple("FUERA_DE_NORMA", "0.0",
                                "No venderlo como cacao de calidad: hacer una selección completa y revisar todo el proceso después de la cosecha."));
    }

    @Test
    @DisplayName("RF-02 · C8 · el administrador queda activo, en una empresa activa y con la contraseña inicial (el inicio de sesión se prueba en RF-01)")
    void elAdministradorPuedeEntrarConLaContrasenaInicial() {
        UUID empresa = api.registrarValida(registro(NIT, CORREO));

        Map<String, Object> admin = jdbc.queryForMap("""
                SELECT u.contrasena_hash, u.activo, u.rol, e.activa FROM usuario u
                JOIN empresa e ON e.id = u.empresa_id WHERE u.empresa_id = ?""", empresa);

        assertThat(admin)
                .containsEntry("activo", true)
                .containsEntry("rol", "ADMIN_EMPRESA")
                .containsEntry("activa", true);
        assertThat(BCRYPT.matches(CONTRASENA, (String) admin.get("contrasena_hash"))).isTrue();
    }

    @Test
    @DisplayName("RNF-04 · C3 · la contraseña del administrador se guarda con hash BCrypt y no sale en la respuesta")
    void guardaLaContrasenaConHash() {
        MvcTestResult resultado = api.registrar(registro(NIT, CORREO));

        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        String hash = jdbc.queryForObject("SELECT contrasena_hash FROM usuario WHERE correo = ?", String.class, CORREO);
        assertThat(hash).startsWith("$2").hasSize(60).doesNotContain(CONTRASENA);
        assertThat(resultado.getResponse().getContentAsByteArray()).asString(StandardCharsets.UTF_8)
                .doesNotContain("contrasena")
                .doesNotContain(CONTRASENA)
                .doesNotContain(hash);
    }

    @Test
    @DisplayName("RP-11 · con Accept-Language: en, el error del dígito de verificación sale en inglés")
    void losMensajesSalenEnIngles() {
        MvcTestResult resultado = mvc.post().uri(ApiEmpresas.RUTA)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ApiEmpresas.aJson(registro("900123456-7", CORREO)))
                .with(csrf())
                .with(ApiEmpresas.adminPlataforma())
                .exchange();

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(tuple("nit", "The NIT check digit is not valid."));
    }

    /**
     * Prueba de límite de la contraseña inicial: sin error esperado, la empresa queda registrada y el
     * hash coincide; con error, la API responde 400 solo con ese mensaje y no guarda nada.
     */
    private void comprobarContrasena(String contrasena, String error) {
        Map<String, Object> datos = registro(NIT, CORREO);
        administrador(datos).put("contrasena", contrasena);

        MvcTestResult resultado = api.registrar(datos);

        if (error == null) {
            assertThat(resultado).hasStatus(HttpStatus.CREATED);
            String hash = jdbc.queryForObject("SELECT contrasena_hash FROM usuario WHERE correo = ?", String.class, CORREO);
            assertThat(BCRYPT.matches(contrasena, hash)).isTrue();
        } else {
            assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(errores(resultado)).containsExactly(tuple("administrador.contrasena", error));
            assertThat(contar(jdbc, "empresa")).isZero();
            assertThat(contar(jdbc, "usuario")).isZero();
        }
    }
}
