/*
 * Nombre: UsuariosPorEmpresaIT.java
 * Descripción: Pruebas de seguridad de la gestión de usuarios entre empresas: cada administrador solo ve
 *              y modifica los usuarios de su empresa (RF-03, RNF-03).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario;

import static co.edu.usco.inspectia.usuario.ApiUsuarios.NO_EXISTE;
import static co.edu.usco.inspectia.usuario.ApiUsuarios.datos;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA;
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
 * Pruebas de seguridad entre empresas (RNF-03, RP-12): el administrador de Cacaos del Huila no ve ni
 * modifica a los usuarios de Cacao del Sur, ni puede crear usuarios en ella. La empresa sale siempre
 * de la sesión, nunca de la petición.
 */
@PruebaIntegracion
class UsuariosPorEmpresaIT {

    private static final String ADMIN = "admin@cacaos.test";
    private static final String MARTA = "marta@cacaosur.test";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;
    private ApiUsuarios usuarios;
    private UUID empresa;
    private UUID otraEmpresa;
    private UUID deOtraEmpresa;
    private String token;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        usuarios = new ApiUsuarios(api);
        ApiSesion.borrarDatos(jdbc);
        empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        api.crearUsuario(empresa, "Ana Pérez", ADMIN, "ADMIN_EMPRESA");
        api.crearUsuario(empresa, "Luis Gómez", "luis@cacaos.test", "USUARIO_FINAL");
        otraEmpresa = api.crearEmpresa("Cacao del Sur", "800765432-1");
        api.crearUsuario(otraEmpresa, "Jorge Silva", "jorge@cacaosur.test", "ADMIN_EMPRESA");
        deOtraEmpresa = api.crearUsuario(otraEmpresa, "Marta Ruiz", MARTA, "USUARIO_FINAL");
        api.crearUsuario(null, "Equipo InspectIA", "plataforma@inspectia.test", "ADMIN_PLATAFORMA");
        token = api.token(ADMIN);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    @Test
    @DisplayName("RF-03 · C9 · el administrador no ve ni modifica usuarios de otra empresa: la API responde que no existen")
    void otraEmpresaNoExiste() {
        MvcTestResult ver = usuarios.ver(token, deOtraEmpresa);
        MvcTestResult editar = usuarios.editar(token, deOtraEmpresa,
                datos("Cambiado", MARTA, "ADMIN_EMPRESA", "Otra-Clave-2026"));
        MvcTestResult desactivar = usuarios.desactivar(token, deOtraEmpresa);
        MvcTestResult reactivar = usuarios.reactivar(token, deOtraEmpresa);

        for (MvcTestResult resultado : List.of(ver, editar, desactivar, reactivar)) {
            assertThat(resultado).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(json(resultado)).containsEntry("detail", NO_EXISTE);
        }
        assertThat(jdbc.queryForMap("SELECT nombre, rol, activo FROM usuario WHERE id = ?", deOtraEmpresa))
                .containsEntry("nombre", "Marta Ruiz").containsEntry("rol", "USUARIO_FINAL").containsEntry("activo", true);
        assertThat(api.iniciar(MARTA, CONTRASENA)).as("su contraseña no cambió").hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RNF-03 · C2 · el listado de usuarios solo trae los de la empresa del administrador")
    void elListadoNoMezclaEmpresas() {
        MvcTestResult listado = usuarios.listar(token);

        assertThat(listado).hasStatus(HttpStatus.OK);
        String cuerpo = new String(listado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        List<String> correos = JsonPath.read(cuerpo, "$.contenido[*].correo");
        assertThat(correos).as("por nombre, sin los de otra empresa ni el de la plataforma")
                .containsExactly(ADMIN, "luis@cacaos.test");
        assertThat(json(listado)).containsEntry("totalElementos", 2);
    }

    @Test
    @DisplayName("RNF-03 · C5 · la empresa del usuario nuevo sale de la sesión: si la petición trae otra, la API la ignora (prueba de seguridad)")
    void laEmpresaSaleDeLaSesion() {
        Map<String, Object> datos = datos("Pedro Díaz", "pedro@cacaos.test", "USUARIO_FINAL", CONTRASENA);
        datos.put("empresaId", otraEmpresa.toString());
        datos.put("empresa", Map.of("id", otraEmpresa.toString()));

        MvcTestResult creado = usuarios.crear(token, datos);

        assertThat(creado).hasStatus(HttpStatus.CREATED);
        UUID id = UUID.fromString((String) json(creado).get("id"));
        assertThat(jdbc.queryForObject("SELECT empresa_id FROM usuario WHERE id = ?", UUID.class, id)).isEqualTo(empresa);
    }
}
