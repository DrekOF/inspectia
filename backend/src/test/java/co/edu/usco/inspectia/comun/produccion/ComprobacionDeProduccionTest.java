/*
 * Nombre: ComprobacionDeProduccionTest.java
 * Descripción: Pruebas unitarias de la comprobación del perfil produccion (D-16): solo pasa con el almacén en S3 y el
 *              servicio de IA, y dice todo lo que falta en un mensaje.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.produccion;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** La comprobación del perfil produccion (D-16). */
class ComprobacionDeProduccionTest {

    @Test
    @DisplayName("D-16 · con inspectia.archivos.almacen=s3 e inspectia.ia.clasificador=servicio, sin distinguir mayúsculas como Spring, la comprobación pasa")
    void pasaConS3YElServicio() {
        assertThatCode(() -> ComprobacionDeProduccion.revisar(new MockEnvironment()
                .withProperty("inspectia.archivos.almacen", "s3").withProperty("inspectia.ia.clasificador", "servicio")))
                .doesNotThrowAnyException();
        assertThatCode(() -> ComprobacionDeProduccion.revisar(new MockEnvironment()
                .withProperty("inspectia.archivos.almacen", "S3").withProperty("inspectia.ia.clasificador", "Servicio")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("D-16 · sin ninguno de los dos, el mensaje dice las dos variables que faltan y que no están")
    void sinNingunoDiceLasDos() {
        assertThatThrownBy(() -> ComprobacionDeProduccion.revisar(new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INSPECTIA_ARCHIVOS_ALMACEN=s3")
                .hasMessageContaining("INSPECTIA_IA_CLASIFICADOR=servicio")
                .hasMessageContaining("vacío (no está)");
    }

    @Test
    @DisplayName("D-16 · otro almacén, como la carpeta, o un valor con espacios, no pasa; tampoco el clasificador falso")
    void otrosValoresNoPasan() {
        assertThatThrownBy(() -> ComprobacionDeProduccion.revisar(new MockEnvironment()
                .withProperty("inspectia.archivos.almacen", "carpeta")
                .withProperty("inspectia.ia.clasificador", "servicio")))
                .hasMessageContaining("«carpeta»").hasMessageNotContaining("INSPECTIA_IA_CLASIFICADOR");
        assertThatThrownBy(() -> ComprobacionDeProduccion.revisar(new MockEnvironment()
                .withProperty("inspectia.archivos.almacen", " s3")
                .withProperty("inspectia.ia.clasificador", "servicio")))
                .hasMessageContaining("INSPECTIA_ARCHIVOS_ALMACEN");
        assertThatThrownBy(() -> ComprobacionDeProduccion.revisar(new MockEnvironment()
                .withProperty("inspectia.archivos.almacen", "s3")
                .withProperty("inspectia.ia.clasificador", "falso")))
                .hasMessageContaining("«falso»").hasMessageNotContaining("INSPECTIA_ARCHIVOS_ALMACEN");
    }
}
