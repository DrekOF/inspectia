/*
 * Nombre: LlavesDeArchivosTest.java
 * Descripción: Pruebas unitarias de las dos formas de llave que aceptan los almacenes de archivos: la de una foto y la
 *              de un reporte PDF (D-16, D-41); cualquier otra se rechaza.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Las formas de llave de los archivos (D-16, D-41). */
class LlavesDeArchivosTest {

    private static final String EMPRESA = "0199f0a0-0000-7000-8000-000000000001";
    private static final String LOTE = "0199f0a0-0000-7000-8000-000000000002";
    private static final String UUID = "0199f0a0-0000-7000-8000-000000000003";
    private static final String FOTO = EMPRESA + "/" + LOTE + "/m1-" + UUID + ".jpg";
    private static final String REPORTE = "reportes/" + EMPRESA + "/" + LOTE + "/" + UUID + "-es.pdf";

    @Test
    @DisplayName("D-16 · una foto es <empresa>/<lote>/m<1|2>-<uuid>.<jpg|png> y un reporte, reportes/<empresa>/<lote>/<uuid>-<es|en>.pdf")
    void lasDosFormas() {
        assertThat(LlavesDeArchivos.clase(FOTO)).isEqualTo(LlavesDeArchivos.Clase.FOTO);
        assertThat(LlavesDeArchivos.clase(EMPRESA + "/" + LOTE + "/m2-" + UUID + ".png"))
                .isEqualTo(LlavesDeArchivos.Clase.FOTO);
        assertThat(LlavesDeArchivos.clase(REPORTE)).isEqualTo(LlavesDeArchivos.Clase.REPORTE);
        assertThat(LlavesDeArchivos.clase("reportes/" + EMPRESA + "/" + LOTE + "/" + UUID + "-en.pdf"))
                .isEqualTo(LlavesDeArchivos.Clase.REPORTE);
        assertThat(LlavesDeArchivos.esFoto(FOTO)).isTrue();
        assertThat(LlavesDeArchivos.esFoto(REPORTE)).isFalse();
    }

    @Test
    @DisplayName("D-16 · cualquier otra llave se rechaza: sin la muestra, con otra extensión o idioma, en otra carpeta, con «..», en mayúsculas, vacía o nula")
    void otrasFormasSeRechazan() {
        String sinMuestra = EMPRESA + "/" + LOTE + "/" + UUID + ".jpg";
        for (String llave : new String[] {sinMuestra, EMPRESA + "/" + LOTE + "/m3-" + UUID + ".jpg",
            EMPRESA + "/" + LOTE + "/m1-" + UUID + ".gif", "fotos/" + FOTO, "../" + FOTO, FOTO.toUpperCase(),
            "reportes/" + EMPRESA + "/" + LOTE + "/" + UUID + "-fr.pdf",
            "reportes/" + EMPRESA + "/" + LOTE + "/" + UUID + ".pdf", "respaldos/" + EMPRESA + "/x.sql",
            "modelos/inspectia_EfficientNet-B0_v1.0_savedmodel.zip", "reportes/../" + FOTO, "", null}) {
            assertThatThrownBy(() -> LlavesDeArchivos.clase(llave)).as(String.valueOf(llave))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(LlavesDeArchivos.esFoto(llave)).as(String.valueOf(llave)).isFalse();
        }
    }
}
