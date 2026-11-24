/*
 * Nombre: AlmacenEnCarpetaTest.java
 * Descripción: Pruebas unitarias del almacén de las fotos en una carpeta (D-41): los enlaces relativos, las llaves que
 *              no salen de la carpeta, los archivos que no se pisan, la lectura para el clasificador y el borrado que
 *              no falla; las llaves llevan la muestra para la que se pidieron (D-25). Los reportes PDF se guardan
 *              con su propia llave y no tienen enlaces (D-16, RF-15).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */
package co.edu.usco.inspectia.analisis.adaptadores.carpeta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.usco.inspectia.RelojDePrueba;
import co.edu.usco.inspectia.analisis.aplicacion.ArchivoGuardado;
import co.edu.usco.inspectia.analisis.aplicacion.EnlaceTemporal;
import co.edu.usco.inspectia.comun.errores.ConflictoExcepcion;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** El almacén en una carpeta temporal (D-41). */
class AlmacenEnCarpetaTest {

    private static final Instant AHORA = Instant.parse("2026-11-23T15:00:00Z");
    private static final String EMPRESA = "0199f0a0-0000-7000-8000-000000000001";
    private static final String LOTE = "0199f0a0-0000-7000-8000-000000000002";
    private static final String ARCHIVO = "m1-0199f0a0-0000-7000-8000-000000000003.jpg";
    private static final String LLAVE = EMPRESA + "/" + LOTE + "/" + ARCHIVO;
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4, 5};
    private static final String REPORTE = "reportes/" + EMPRESA + "/" + LOTE
            + "/0199f0a0-0000-7000-8000-000000000004-es.pdf";
    private static final byte[] PDF = "%PDF-1.7 reporte".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    private Path carpeta;

    private AlmacenEnCarpeta almacen;
    private FirmaDeEnlaces firma;

    @BeforeEach
    void preparar() {
        RelojDePrueba reloj = new RelojDePrueba(AHORA);
        firma = new FirmaDeEnlaces(FirmaDeEnlaces.claveAlAzar(), reloj);
        almacen = new AlmacenEnCarpeta(carpeta, firma, reloj);
    }

    @Test
    @DisplayName("D-41 · los enlaces son relativos a /api/v1/archivos, con su vencimiento y su firma, y el de subida pide el tipo firmado")
    void enlacesRelativos() {
        EnlaceTemporal subida = almacen.enlaceDeSubida(LLAVE, "image/jpeg", JPG.length, Duration.ofMinutes(10));
        EnlaceTemporal vista = almacen.enlaceDeVista(LLAVE, Duration.ofMinutes(15));

        long vence = AHORA.plus(Duration.ofMinutes(10)).getEpochSecond();
        assertThat(subida.url()).isEqualTo(AlmacenEnCarpeta.RUTA + "/" + LLAVE + "?vence=" + vence + "&firma="
                + firma.firmarSubida(LLAVE, vence, "image/jpeg", JPG.length));
        assertThat(subida.metodo()).isEqualTo("PUT");
        assertThat(subida.encabezados()).isEqualTo(Map.of("Content-Type", "image/jpeg"));
        assertThat(subida.vence()).isEqualTo(Instant.ofEpochSecond(vence));
        assertThat(vista.url()).startsWith(AlmacenEnCarpeta.RUTA + "/" + LLAVE + "?vence=");
        assertThat(vista.metodo()).isEqualTo("GET");
    }

    @Test
    @DisplayName("D-41 · una llave sin la forma <empresa>/<lote>/m<muestra>-<archivo>, con la muestra 1 o 2, como la de antes sin la muestra, o que sale de la carpeta, se rechaza")
    void llavesFueraDeLaCarpeta() {
        String sinMuestra = ARCHIVO.substring("m1-".length());
        for (String llave : new String[] {"../" + LLAVE, EMPRESA + "/../" + LOTE + "/x.jpg", "/etc/passwd",
            EMPRESA + "/" + LOTE + "/archivo.exe", LLAVE.toUpperCase(), "", null, EMPRESA + "/" + LOTE + "/" + sinMuestra,
            EMPRESA + "/" + LOTE + "/m3-" + sinMuestra, EMPRESA + "/" + LOTE + "/m0-" + sinMuestra}) {
            assertThatThrownBy(() -> almacen.ruta(llave)).as(String.valueOf(llave))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(AlmacenEnCarpeta.llave(EMPRESA, LOTE, "..")).isEmpty();
        assertThat(AlmacenEnCarpeta.llave(EMPRESA, LOTE, sinMuestra)).isEmpty();
        assertThat(AlmacenEnCarpeta.llave(EMPRESA, LOTE, ARCHIVO)).contains(LLAVE);
        assertThat(AlmacenEnCarpeta.llave(EMPRESA, LOTE, "m2-" + sinMuestra)).contains(EMPRESA + "/" + LOTE + "/m2-"
                + sinMuestra);
        assertThat(almacen.ruta(LLAVE).startsWith(carpeta.toAbsolutePath().normalize())).isTrue();
    }

    @Test
    @DisplayName("D-41 · un archivo guardado se describe con su tamaño y sus primeros bytes, y no se pisa: el segundo responde 409")
    void guardarYNoPisar() throws Exception {
        almacen.guardar(LLAVE, JPG);

        ArchivoGuardado guardado = almacen.describir(LLAVE).orElseThrow();
        assertThat(guardado.bytes()).isEqualTo(JPG.length);
        assertThat(guardado.primeros()).hasSize(8).startsWith((byte) 0xFF, (byte) 0xD8, (byte) 0xFF);
        assertThatThrownBy(() -> almacen.guardar(LLAVE, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 9}))
                .isInstanceOf(ConflictoExcepcion.class);
        assertThat(Files.readAllBytes(almacen.ruta(LLAVE))).isEqualTo(JPG);
        try (var temporales = Files.list(almacen.ruta(LLAVE).getParent())) {
            assertThat(temporales.map(ruta -> ruta.getFileName().toString())).containsExactly(ARCHIVO);
        }
    }

    @Test
    @DisplayName("RF-05 · C5 · borrar una foto la quita de la carpeta, y borrar una que ya no está no falla")
    void borrar() {
        almacen.guardar(LLAVE, JPG);

        almacen.borrar(LLAVE);
        almacen.borrar(LLAVE);

        assertThat(almacen.describir(LLAVE)).isEmpty();
        assertThat(almacen.existe(LLAVE)).isFalse();
    }

    @Test
    @DisplayName("D-41 · leer una foto guardada devuelve todo su contenido, para mandárselo al clasificador; una que no está, no se puede leer")
    void leer() {
        almacen.guardar(LLAVE, JPG);

        assertThat(almacen.leer(LLAVE)).isEqualTo(JPG);
        almacen.borrar(LLAVE);
        assertThatThrownBy(() -> almacen.leer(LLAVE)).isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(() -> almacen.leer("../" + LLAVE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("D-16 · la carpeta guarda un archivo que arma el servidor, como el reporte PDF con su llave reportes/<empresa>/<lote>/<uuid>-<idioma>.pdf: lo describe, lo lee y lo borra")
    void guardarUnReporte() {
        almacen.guardar(REPORTE, PDF, "application/pdf");

        assertThat(almacen.leer(REPORTE)).isEqualTo(PDF);
        assertThat(almacen.describir(REPORTE).orElseThrow().bytes()).isEqualTo(PDF.length);
        assertThat(almacen.ruta(REPORTE)).isEqualTo(carpeta.toAbsolutePath().normalize().resolve(REPORTE));
        almacen.borrar(REPORTE);
        assertThat(almacen.describir(REPORTE)).isEmpty();
    }

    @Test
    @DisplayName("D-16 · guardar con el tipo tampoco pisa un archivo: con la llave al azar del servidor, sería un error del servidor; y una llave con otra forma se rechaza")
    void guardarConTipoNoPisa() {
        almacen.guardar(LLAVE, JPG, "image/jpeg");

        assertThatThrownBy(() -> almacen.guardar(LLAVE, PDF, "image/jpeg")).isInstanceOf(IllegalStateException.class);
        assertThat(almacen.leer(LLAVE)).isEqualTo(JPG);
        assertThatThrownBy(() -> almacen.guardar("reportes/" + EMPRESA + "/x.pdf", PDF, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> almacen.guardar("../" + REPORTE, PDF, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("D-16 · los enlaces firmados son solo de fotos: un reporte se descarga por el backend, con la sesión, y no tiene enlace")
    void unReporteNoTieneEnlace() {
        assertThatThrownBy(() -> almacen.enlaceDeVista(REPORTE, Duration.ofMinutes(15)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> almacen.enlaceDeSubida(REPORTE, "application/pdf", PDF.length, Duration.ofMinutes(10)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
