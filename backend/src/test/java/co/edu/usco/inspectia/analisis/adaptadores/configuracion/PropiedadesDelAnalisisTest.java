/*
 * Nombre: PropiedadesDelAnalisisTest.java
 * Descripción: Pruebas de los parámetros del análisis de application.yml: los de hoy arrancan, y uno que falta o que no
 *              sirve no deja arrancar el servidor (P-15, P-19, D-24, RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.analisis.dominio.ParametrosDelAnalisis;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

/** Los parámetros del análisis se validan al arrancar. */
class PropiedadesDelAnalisisTest {

    private static final String[] DE_HOY = {"inspectia.analisis.umbral-confianza-baja=60",
        "inspectia.analisis.porcentaje-max-dudosos=10", "inspectia.analisis.factor-del-margen=1",
        "inspectia.analisis.puntos-para-otra-muestra=1"};

    private ApplicationContextRunner contexto() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
                .withUserConfiguration(ConfiguracionDelAnalisis.class);
    }

    @Test
    @DisplayName("P-15 · con los parámetros de hoy (60, 10 %, k = 1 y 1 punto) el servidor arranca, con la cola automática revisando cada 5 s")
    void parametrosDeHoy() {
        contexto().withPropertyValues(DE_HOY).run(cargado -> {
            assertThat(cargado).hasNotFailed();
            assertThat(cargado.getBean(ParametrosDelAnalisis.class)).isEqualTo(new ParametrosDelAnalisis(
                    new BigDecimal("60"), new BigDecimal("10"), BigDecimal.ONE, BigDecimal.ONE));
            PropiedadesDelAnalisis.Cola cola = cargado.getBean(PropiedadesDelAnalisis.class).cola();
            assertThat(cola.automatica()).isTrue();
            assertThat(cola.esperaMaxima()).isEqualTo(Duration.ofSeconds(5));
        });
    }

    @Test
    @DisplayName("P-15 · sin un parámetro, el servidor no arranca")
    void faltaUnParametro() {
        contexto().withPropertyValues(DE_HOY[0], DE_HOY[1], DE_HOY[2])
                .run(cargado -> assertThat(cargado).hasFailed());
    }

    @Test
    @DisplayName("P-15 · un parámetro que no cabe en su rango o en sus decimales no deja arrancar el servidor (prueba de límite)")
    void parametroQueNoSirve() {
        String[][] malos = {
            {"inspectia.analisis.umbral-confianza-baja=100.01"},
            {"inspectia.analisis.umbral-confianza-baja=59.995"},
            {"inspectia.analisis.porcentaje-max-dudosos=-1"},
            {"inspectia.analisis.porcentaje-max-dudosos=10.05"},
            {"inspectia.analisis.factor-del-margen=0"},
            {"inspectia.analisis.puntos-para-otra-muestra=0"},
            {"inspectia.analisis.cola.espera-maxima=0s"}};
        for (String[] malo : malos) {
            contexto().withPropertyValues(DE_HOY).withPropertyValues(malo)
                    .run(cargado -> assertThat(cargado).as(malo[0]).hasFailed());
        }
        contexto().withPropertyValues(DE_HOY).withPropertyValues("inspectia.analisis.umbral-confianza-baja=100",
                "inspectia.analisis.porcentaje-max-dudosos=0").run(cargado -> assertThat(cargado).hasNotFailed());
    }

    @Test
    @DisplayName("RNF-02 · la cola se puede atender a mano (automatica=false) y revisar con otra espera")
    void colaAMano() {
        contexto().withPropertyValues(DE_HOY).withPropertyValues("inspectia.analisis.cola.automatica=false",
                "inspectia.analisis.cola.espera-maxima=250ms").run(cargado -> {
                    PropiedadesDelAnalisis.Cola cola = cargado.getBean(PropiedadesDelAnalisis.class).cola();
                    assertThat(cola.automatica()).isFalse();
                    assertThat(cola.esperaMaxima()).isEqualTo(Duration.ofMillis(250));
                });
    }
}
