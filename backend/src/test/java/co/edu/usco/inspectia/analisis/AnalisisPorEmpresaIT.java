/*
 * Nombre: AnalisisPorEmpresaIT.java
 * Descripción: Pruebas de la separación entre empresas en el análisis (RNF-03, RP-12): enviar, ver el avance y ver el
 *              resultado de un lote de otra empresa responden que no existe, sin cambiar nada.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.analisis.aplicacion.AtenderCola;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
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
 * El análisis es de la empresa (RNF-03). Luis Gómez, de Cacaos del Huila, analiza su lote, y Marta Ruiz, de Cacao del
 * Sur, y su administradora, Eva Ríos, intentan llegar a él.
 */
@PruebaIntegracion
class AnalisisPorEmpresaIT {

    private static final String NO_EXISTE = "No existe un registro con ese identificador.";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AtenderCola atender;

    private ApiAnalisis analisis;
    private String tokenDeLuis;
    private String tokenDeMarta;
    private String tokenDeEva;
    private UUID loteDeLuis;

    @BeforeEach
    void preparar() {
        ApiSesion api = new ApiSesion(mvc, jdbc);
        analisis = new ApiAnalisis(api, new ApiFotos(api, mvc));
        ApiSesion.borrarDatos(jdbc);
        api.crearUsuario(api.crearEmpresa("Cacaos del Huila", "900123456-8"), "Luis Gómez", "luis@cacaos.test",
                "USUARIO_FINAL");
        UUID otraEmpresa = api.crearEmpresa("Cacao del Sur", "800765432-1");
        api.crearUsuario(otraEmpresa, "Marta Ruiz", "marta@cacaosur.test", "USUARIO_FINAL");
        api.crearUsuario(otraEmpresa, "Eva Ríos", "eva@cacaosur.test", "ADMIN_EMPRESA");
        tokenDeLuis = api.token("luis@cacaos.test");
        tokenDeMarta = api.token("marta@cacaosur.test");
        tokenDeEva = api.token("eva@cacaosur.test");
        loteDeLuis = new ApiLotes(api).registrarOk(tokenDeLuis, "L-001");
        analisis.subirFotos(tokenDeLuis, loteDeLuis, new int[] {100, 100}, new int[] {100, 100});
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    @Test
    @DisplayName("RNF-03 · C1 · enviar a analizar un lote de otra empresa responde que no existe, y su muestra sigue pendiente")
    void enviarElDeOtraEmpresa() {
        MvcTestResult resultado = analisis.enviar(tokenDeMarta, loteDeLuis);

        assertThat(resultado).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(json(resultado)).containsEntry("detail", NO_EXISTE);
        assertThat(jdbc.queryForObject("SELECT estado FROM muestra WHERE lote_id = ?", String.class, loteDeLuis))
                .isEqualTo("PENDIENTE");
        assertThat(atender.atenderUna()).as("nada entró a la cola").isFalse();
    }

    @Test
    @DisplayName("RNF-03 · C1 · el avance y el resultado de un lote de otra empresa responden que no existe, al usuario final y a la administradora")
    void verElDeOtraEmpresa() {
        analisis.enviarOk(tokenDeLuis, loteDeLuis);
        atender.atenderUna();

        for (String token : new String[] {tokenDeMarta, tokenDeEva}) {
            MvcTestResult avance = analisis.avance(token, loteDeLuis);
            MvcTestResult resultado = analisis.resultado(token, loteDeLuis);

            assertThat(avance).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(json(avance)).containsEntry("detail", NO_EXISTE);
            assertThat(resultado).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(json(resultado)).containsEntry("detail", NO_EXISTE);
        }
        assertThat(analisis.resultado(tokenDeLuis, loteDeLuis)).hasStatus(HttpStatus.OK);
    }
}
