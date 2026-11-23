/*
 * Nombre: ConfiguracionDelClasificadorFalsoTest.java
 * Descripción: Pruebas de cuándo se carga el clasificador falso y su registro en la tabla modelo: solo con
 *              inspectia.ia.clasificador=falso, y con una demora que no sea negativa (RF-08, D-39).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.falso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeModelos;
import co.edu.usco.inspectia.analisis.dominio.RecallsDelModelo;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Cuándo se carga el clasificador falso. Como el almacén en carpeta, no debe llegar a un despliegue sin que alguien lo
 * pida: no tiene valor por defecto.
 */
class ConfiguracionDelClasificadorFalsoTest {

    private final RepositorioDeModelos modelos = mock(RepositorioDeModelos.class);

    private ApplicationContextRunner contexto() {
        return new ApplicationContextRunner()
                .withBean(RepositorioDeModelos.class, () -> modelos)
                .withUserConfiguration(ConfiguracionDelClasificadorFalso.class);
    }

    @Test
    @DisplayName("RF-08 · con inspectia.ia.clasificador=falso se carga el clasificador falso y registra su modelo, con los recall de EfficientNet-B0, al arrancar")
    void conElFalso() {
        contexto().withPropertyValues("inspectia.ia.clasificador=falso").run(cargado -> {
            assertThat(cargado).hasNotFailed().hasSingleBean(ClasificadorFalso.class);
            verify(modelos).registrarSiFalta("clasificador-falso", "1.0",
                    new RecallsDelModelo(new BigDecimal("91.20"), new BigDecimal("53.40")));
        });
    }

    @Test
    @DisplayName("RF-08 · sin inspectia.ia.clasificador, o con otro valor, no hay clasificador falso ni se registra su modelo")
    void sinElFalso() {
        contexto().run(cargado -> assertThat(cargado).doesNotHaveBean(ClasificadorFalso.class));
        contexto().withPropertyValues("inspectia.ia.clasificador=servicio").run(cargado ->
                assertThat(cargado).doesNotHaveBean(ClasificadorFalso.class));
        verify(modelos, never()).registrarSiFalta(any(), any(), any());
    }

    @Test
    @DisplayName("RNF-01 · la demora del clasificador falso no puede ser negativa: el servidor no arranca")
    void demoraNegativa() {
        contexto().withPropertyValues("inspectia.ia.clasificador=falso", "inspectia.ia.falso.demora=-1s")
                .run(cargado -> assertThat(cargado).hasFailed());
        contexto().withPropertyValues("inspectia.ia.clasificador=falso", "inspectia.ia.falso.demora=2s")
                .run(cargado -> assertThat(cargado).hasNotFailed());
    }
}
