/*
 * Nombre: LoteTest.java
 * Descripción: Pruebas unitarias de las reglas del registro de un lote en el dominio, sin Spring (RF-04).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Las reglas del registro de un lote (RF-04), en el dominio, sin la base ni la API. */
class LoteTest {

    private static final UUID EMPRESA = UUID.fromString("0190f3a0-0000-7000-8000-000000000001");
    private static final UUID USUARIO = UUID.fromString("0190f3a0-0000-7000-8000-000000000002");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 9);

    @Test
    @DisplayName("RF-04 · C1 · un lote recién registrado queda pendiente de análisis, con la empresa y el usuario que lo registra")
    void quedaPendiente() {
        Lote lote = Lote.registrar(EMPRESA, USUARIO, "L-001", HOY.minusDays(7), "Finca La Esperanza", null, HOY);

        assertThat(lote.getEstado()).isEqualTo(EstadoDeLote.PENDIENTE);
        assertThat(lote.getEmpresaId()).isEqualTo(EMPRESA);
        assertThat(lote.getUsuarioId()).isEqualTo(USUARIO);
        assertThat(lote.getCodigo()).isEqualTo("L-001");
        assertThat(lote.getFechaRecepcion()).isEqualTo(HOY.minusDays(7));
        assertThat(lote.getOrigen()).isEqualTo("Finca La Esperanza");
        assertThat(lote.getObservaciones()).isNull();
    }

    @Test
    @DisplayName("RF-04 · C4 · el dominio rechaza una fecha de recepción posterior a hoy y acepta la de hoy (prueba de límite)")
    void fechaNoPosteriorAHoy() {
        assertThatThrownBy(() -> Lote.registrar(EMPRESA, USUARIO, "L-001", HOY.plusDays(1), "Finca La Esperanza",
                null, HOY)).isInstanceOf(FechaDeRecepcionFuturaExcepcion.class);
        assertThat(Lote.registrar(EMPRESA, USUARIO, "L-001", HOY, "Finca La Esperanza", null, HOY).getFechaRecepcion())
                .isEqualTo(HOY);
    }

    @Test
    @DisplayName("RF-04 · C3 · «l-001 » y «L-001» tienen la misma clave de código; «L-0001» tiene otra")
    void claveDeCodigo() {
        assertThat(Lote.claveDeCodigo("l-001 ")).isEqualTo(Lote.claveDeCodigo("L-001")).isEqualTo("l-001");
        assertThat(Lote.claveDeCodigo("L-0001")).isNotEqualTo(Lote.claveDeCodigo("L-001"));
    }
}
