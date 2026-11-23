/*
 * Nombre: FirmaDeEnlacesTest.java
 * Descripción: Pruebas unitarias de la firma de los enlaces de la carpeta de las fotos (D-41): el vencimiento, lo que
 *              ata la firma y la clave.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.carpeta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.usco.inspectia.RelojDePrueba;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** La firma de los enlaces (D-41), con el reloj fijo. */
class FirmaDeEnlacesTest {

    private static final Instant AHORA = Instant.parse("2026-11-23T15:00:00Z");
    private static final String LLAVE = "0199f0a0-0000-7000-8000-000000000001/0199f0a0-0000-7000-8000-000000000002/"
            + "0199f0a0-0000-7000-8000-000000000003.jpg";
    private static final String JPG = "image/jpeg";

    private final RelojDePrueba reloj = new RelojDePrueba(AHORA);
    private final FirmaDeEnlaces firma = new FirmaDeEnlaces(FirmaDeEnlaces.claveAlAzar(), reloj);

    @Test
    @DisplayName("D-41 · el enlace sirve hasta el segundo anterior a su vencimiento y vence en ese segundo (prueba de límite)")
    void venceSinTolerancia() {
        long vence = AHORA.getEpochSecond() + 600;
        String deSubida = firma.firmarSubida(LLAVE, vence, JPG, 1_000);
        String deVista = firma.firmarVista(LLAVE, vence);

        reloj.fijar(Instant.ofEpochSecond(vence - 1));
        assertThat(firma.esSubidaValida(LLAVE, vence, JPG, 1_000, deSubida)).isTrue();
        assertThat(firma.esVistaValida(LLAVE, vence, deVista)).isTrue();
        reloj.fijar(Instant.ofEpochSecond(vence));
        assertThat(firma.esSubidaValida(LLAVE, vence, JPG, 1_000, deSubida)).isFalse();
        assertThat(firma.esVistaValida(LLAVE, vence, deVista)).isFalse();
    }

    @Test
    @DisplayName("D-41 · cambiar el método, la llave, el tipo, el tamaño, el vencimiento o la firma invalida el enlace")
    void laFirmaAtaCadaDato() {
        long vence = AHORA.getEpochSecond() + 600;
        String deSubida = firma.firmarSubida(LLAVE, vence, JPG, 1_000);
        String otraLlave = LLAVE.replace("000000000003.jpg", "000000000004.jpg");

        assertThat(firma.esSubidaValida(LLAVE, vence, JPG, 1_000, deSubida)).isTrue();
        assertThat(firma.esVistaValida(LLAVE, vence, deSubida)).as("otro método").isFalse();
        assertThat(firma.esSubidaValida(otraLlave, vence, JPG, 1_000, deSubida)).as("otra llave").isFalse();
        assertThat(firma.esSubidaValida(LLAVE, vence, "image/png", 1_000, deSubida)).as("otro tipo").isFalse();
        assertThat(firma.esSubidaValida(LLAVE, vence, JPG, 1_001, deSubida)).as("otro tamaño").isFalse();
        assertThat(firma.esSubidaValida(LLAVE, vence + 1, JPG, 1_000, deSubida)).as("otro vencimiento").isFalse();
        assertThat(firma.esSubidaValida(LLAVE, vence, JPG, 1_000, deSubida.substring(1))).as("otra firma").isFalse();
        assertThat(firma.esSubidaValida(LLAVE, vence, JPG, 1_000, "no es Base64!")).as("firma rota").isFalse();
        assertThat(firma.esSubidaValida(LLAVE, vence, JPG, 1_000, null)).as("sin firma").isFalse();
        assertThat(firma.esSubidaValida(LLAVE, vence, null, 1_000, deSubida)).as("sin tipo").isFalse();
    }

    @Test
    @DisplayName("D-41 · una firma hecha con otra clave no sirve: la clave es al azar en cada arranque")
    void otraClaveNoSirve() {
        long vence = AHORA.getEpochSecond() + 600;
        FirmaDeEnlaces otroArranque = new FirmaDeEnlaces(FirmaDeEnlaces.claveAlAzar(), reloj);

        assertThat(firma.esVistaValida(LLAVE, vence, otroArranque.firmarVista(LLAVE, vence))).isFalse();
        assertThat(FirmaDeEnlaces.claveAlAzar()).hasSize(32).isNotEqualTo(FirmaDeEnlaces.claveAlAzar());
    }

    @Test
    @DisplayName("D-41 · una clave de menos de 32 bytes no se acepta")
    void claveCorta() {
        assertThatThrownBy(() -> new FirmaDeEnlaces(new byte[31], reloj)).isInstanceOf(IllegalArgumentException.class);
    }
}
