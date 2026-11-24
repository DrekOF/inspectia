/*
 * Nombre: AlmacenEnCarpetaTest.java
 * Descripción: Pruebas unitarias del almacén de las fotos en una carpeta (D-41): los enlaces relativos, las llaves que
 *              no salen de la carpeta, los archivos que no se pisan, la lectura para el clasificador y el borrado que
 *              no falla; las llaves llevan la muestra para la que se pidieron (D-25); los archivos del reporte PDF, uno
 *              por idioma (RF-15, D-50), que solo se guardan, leen y borran, sin enlaces firmados.
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.4
 */
package co.edu.usco.inspectia.analisis.adaptadores.carpeta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.usco.inspectia.RelojDePrueba;
import co.edu.usco.inspectia.analisis.aplicacion.ArchivoGuardado;
import co.edu.usco.inspectia.analisis.aplicacion.EnlaceTemporal;
import co.edu.usco.inspectia.comun.errores.ConflictoExcepcion;
import java.io.UncheckedIOException;
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
    @DisplayName("RF-15 · el reporte PDF se guarda con la llave reportes/<empresa>/<lote>/<uuid>-<es|en>.pdf, se lee, se describe y se borra, sin pisar uno que ya está (D-50)")
    void guardarUnReporte() {
        String llave = "reportes/" + EMPRESA + "/" + LOTE + "/0199f0a0-0000-7000-8000-000000000009-es.pdf";
        byte[] pdf = "%PDF-1.7 reporte".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        almacen.guardar(llave, pdf, "application/pdf");

        assertThat(carpeta.resolve(llave)).exists();
        assertThat(almacen.leer(llave)).isEqualTo(pdf);
        assertThat(almacen.describir(llave)).get().extracting(ArchivoGuardado::bytes).isEqualTo((long) pdf.length);
        assertThatThrownBy(() -> almacen.guardar(llave, pdf, "application/pdf"))
                .isInstanceOf(IllegalStateException.class);
        almacen.borrar(llave);
        assertThat(almacen.describir(llave)).isEmpty();
    }

    @Test
    @DisplayName("RF-15 · una llave de reporte con otra forma se rechaza: otro idioma, otra extensión, sin la carpeta reportes o que sale de la carpeta (D-50)")
    void llavesDeReporteRechazadas() {
        String base = "reportes/" + EMPRESA + "/" + LOTE + "/0199f0a0-0000-7000-8000-000000000009";
        for (String llave : new String[] {base + "-fr.pdf", base + "-es.jpg", base + ".pdf",
            EMPRESA + "/" + LOTE + "/0199f0a0-0000-7000-8000-000000000009-es.pdf", "reportes/../" + LLAVE,
            "otros/" + EMPRESA + "/" + LOTE + "/0199f0a0-0000-7000-8000-000000000009-es.pdf"}) {
            assertThatThrownBy(() -> almacen.guardar(llave, JPG, "application/pdf")).as(llave)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("RF-15 · un reporte PDF no tiene enlaces firmados ni se sube por el enlace de las fotos: solo lo guarda, lo lee y lo borra el servidor (D-50, RNF-03)")
    void reporteSinEnlaces() {
        String llave = "reportes/" + EMPRESA + "/" + LOTE + "/0199f0a0-0000-7000-8000-000000000009-es.pdf";

        assertThatThrownBy(() -> almacen.enlaceDeVista(llave, Duration.ofMinutes(15)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> almacen.enlaceDeSubida(llave, "application/pdf", 10, Duration.ofMinutes(10)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> almacen.guardar(llave, JPG)).isInstanceOf(IllegalArgumentException.class);
        assertThat(carpeta.resolve(llave)).doesNotExist();
    }
}
