/*
 * Nombre: LotesPorEmpresaIT.java
 * Descripción: Pruebas de seguridad de los lotes entre empresas: un usuario no ve los lotes, las fincas ni
 *              quienes registraron lotes de otra empresa, ni filtra por ellos, y la empresa y el usuario de un lote
 *              salen de la sesión (RNF-03, RP-12, RF-14).
 * Fecha de creación: 10/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiLotes.NO_EXISTE;
import static co.edu.usco.inspectia.analisis.ApiLotes.RECEPCION;
import static co.edu.usco.inspectia.analisis.ApiLotes.datos;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas de seguridad entre empresas (RNF-03, RP-12): Luis Gómez, de Cacaos del Huila, no ve los lotes
 * ni las fincas de Cacao del Sur, ni puede registrar un lote a nombre de ella o de otro usuario. La
 * empresa y el usuario salen siempre de la sesión, nunca de la petición.
 */
@PruebaIntegracion
class LotesPorEmpresaIT {

    private static final String LUIS = "luis@cacaos.test";
    private static final String MARTA = "marta@cacaosur.test";
    private static final String JORGE = "jorge@cacaosur.test";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;
    private ApiLotes lotes;
    private UUID empresa;
    private UUID luis;
    private UUID otraEmpresa;
    private UUID marta;
    private String token;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        lotes = new ApiLotes(api);
        ApiSesion.borrarDatos(jdbc);
        empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        otraEmpresa = api.crearEmpresa("Cacao del Sur", "800765432-1");
        marta = api.crearUsuario(otraEmpresa, "Marta Ruiz", MARTA, "USUARIO_FINAL");
        api.crearUsuario(otraEmpresa, "Jorge Silva", JORGE, "ADMIN_EMPRESA");
        token = api.token(LUIS);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    @Test
    @DisplayName("RNF-03 · C1 · si un usuario pide un lote de otra empresa por su identificador, el sistema responde que no existe")
    void loteDeOtraEmpresaNoExiste() {
        UUID deOtraEmpresa = lotes.registrarOk(api.token(MARTA), "S-001");

        MvcTestResult resultado = lotes.ver(token, deOtraEmpresa);

        assertThat(resultado).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(json(resultado)).containsEntry("detail", NO_EXISTE);
    }

    @Test
    @DisplayName("RNF-03 · C2 · el historial de lotes nunca mezcla empresas: el administrador de Cacao del Sur solo ve los de su empresa")
    void elHistorialNoMezclaEmpresas() {
        lotes.registrarOk(token, "L-001");
        lotes.registrarOk(api.token(MARTA), "S-001");

        MvcTestResult historial = lotes.listar(api.token(JORGE), 0);

        assertThat(historial).hasStatus(HttpStatus.OK);
        List<String> codigos = JsonPath.read(cuerpo(historial), "$.contenido[*].codigo");
        assertThat(codigos).containsExactly("S-001");
        assertThat(json(historial)).containsEntry("totalElementos", 1);
    }

    @Test
    @DisplayName("RNF-03 · C5 · la empresa y el usuario del lote salen de la sesión: si la petición trae otros, la API los ignora (prueba de seguridad)")
    void laEmpresaYElUsuarioSalenDeLaSesion() {
        Map<String, Object> datos = datos("L-001", RECEPCION, "Finca La Esperanza", null);
        datos.put("empresaId", otraEmpresa.toString());
        datos.put("empresa", Map.of("id", otraEmpresa.toString()));
        datos.put("usuarioId", marta.toString());
        datos.put("usuario", Map.of("id", marta.toString()));
        datos.put("estado", "ANALIZADO");

        MvcTestResult registrado = lotes.registrar(token, datos);

        assertThat(registrado).hasStatus(HttpStatus.CREATED);
        UUID id = UUID.fromString((String) json(registrado).get("id"));
        assertThat(jdbc.queryForMap("SELECT empresa_id, usuario_id, estado FROM lote WHERE id = ?", id))
                .containsEntry("empresa_id", empresa)
                .containsEntry("usuario_id", luis)
                .containsEntry("estado", "PENDIENTE");
    }

    @Test
    @DisplayName("RNF-03 · C2 · la lista de quienes registraron lotes no trae a los de otra empresa ni los correos de nadie")
    void losRegistradoresNoMezclanEmpresas() {
        lotes.registrarOk(token, "L-001");
        lotes.registrarOk(api.token(MARTA), "S-001");

        MvcTestResult registradores = lotes.registradores(api.token(JORGE));

        assertThat(registradores).hasStatus(HttpStatus.OK);
        assertThat(ApiSesion.lista(registradores)).containsExactly(
                Map.<String, Object>of("id", marta.toString(), "nombre", "Marta Ruiz"));
        assertThat(cuerpo(registradores)).doesNotContain("@");
        assertThat(ApiSesion.lista(lotes.registradores(token)))
                .containsExactly(Map.<String, Object>of("id", luis.toString(), "nombre", "Luis Gómez"));
    }

    @Test
    @DisplayName("RNF-03 · C2 · filtrar el historial por un usuario de otra empresa da la lista vacía, sin mostrar sus lotes")
    void filtrarPorUnUsuarioDeOtraEmpresa() {
        lotes.registrarOk(token, "L-001");
        lotes.registrarOk(api.token(MARTA), "S-001");

        MvcTestResult historial = lotes.listar(token, Map.of("usuario", marta.toString()));

        assertThat(historial).hasStatus(HttpStatus.OK);
        List<String> codigos = JsonPath.read(cuerpo(historial), "$.contenido[*].codigo");
        assertThat(codigos).isEmpty();
        assertThat(json(historial)).containsEntry("totalElementos", 0);
    }

    @Test
    @DisplayName("RNF-03 · las sugerencias de finca o proveedor no traen las de otra empresa")
    void lasSugerenciasNoMezclanEmpresas() {
        lotes.registrarOk(token, "L-001", "Finca La Esperanza");
        lotes.registrarOk(api.token(MARTA), "S-001", "Finca del Sur");

        MvcTestResult sugeridas = lotes.origenes(token, "finca");

        assertThat(sugeridas).hasStatus(HttpStatus.OK);
        List<String> textos = JsonPath.read(cuerpo(sugeridas), "$");
        assertThat(textos).containsExactly("Finca La Esperanza");
    }

    private static String cuerpo(MvcTestResult resultado) {
        return new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }
}
