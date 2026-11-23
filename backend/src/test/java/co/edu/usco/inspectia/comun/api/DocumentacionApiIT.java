/*
 * Nombre: DocumentacionApiIT.java
 * Descripción: Prueba de la documentación OpenAPI: toda la API pide el token Bearer, salvo iniciar
 *              sesión y los enlaces firmados de los archivos (RP-04, D-28, D-41).
 * Fecha de creación: 14/10/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.comun.api;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Prueba de la documentación OpenAPI ({@code /v3/api-docs}), la que muestra Swagger UI. Con el
 * esquema del token, el botón «Authorize» de Swagger UI recibe el token del inicio de sesión.
 */
@PruebaIntegracion
class DocumentacionApiIT {

    @Autowired
    private MockMvcTester mvc;

    @Test
    @DisplayName("RP-04 · la documentación OpenAPI pide el token Bearer en toda la API, salvo para iniciar sesión y en los enlaces firmados de los archivos (D-41)")
    void documentaElTokenBearer() {
        MvcTestResult resultado = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(resultado).hasStatus(HttpStatus.OK);
        String documento = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        Map<String, Object> esquema = JsonPath.read(documento, "$.components.securitySchemes.token");
        assertThat(esquema).containsEntry("type", "http").containsEntry("scheme", "bearer")
                .containsEntry("bearerFormat", "JWT");
        List<Map<String, Object>> seguridad = JsonPath.read(documento, "$.security");
        assertThat(seguridad).as("toda la API pide el token").containsExactly(Map.of("token", List.of()));
        List<Object> alIniciarSesion = JsonPath.read(documento, "$.paths['/api/v1/sesion'].post.security");
        assertThat(alIniciarSesion).as("iniciar sesión es público").isEmpty();
        Map<String, Object> verSesion = JsonPath.read(documento, "$.paths['/api/v1/sesion'].get");
        assertThat(verSesion).as("ver la sesión pide el token de toda la API").doesNotContainKey("security");
        String archivo = "$.paths['/api/v1/archivos/{empresa}/{lote}/{archivo}']";
        List<Object> alSubir = JsonPath.read(documento, archivo + ".put.security");
        List<Object> alVer = JsonPath.read(documento, archivo + ".get.security");
        assertThat(alSubir).as("los enlaces firmados no piden sesión: los autoriza su firma (D-41)").isEmpty();
        assertThat(alVer).as("los enlaces firmados no piden sesión: los autoriza su firma (D-41)").isEmpty();
    }
}
