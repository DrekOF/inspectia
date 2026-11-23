/*
 * Nombre: RangosDeLaEmpresaJpaTest.java
 * Descripción: Pruebas unitarias de los rangos vigentes de una empresa, con Mockito: los de la empresa con el código de
 *              su categoría, y los de por defecto en las categorías que le falten, sin guardarlos (RF-12, RF-13).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.analisis.dominio.RangoDeCategoria;
import co.edu.usco.inspectia.rango.RangoRecomendacion;
import co.edu.usco.inspectia.rango.RangoRecomendacionCategoria;
import co.edu.usco.inspectia.rango.RangoRecomendacionRepositorio;
import co.edu.usco.inspectia.rango.RangoRecomendacionValoresIniciales;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Los rangos vigentes de una empresa (RF-13), desde el repositorio generado. */
class RangosDeLaEmpresaJpaTest {

    private static final UUID EMPRESA = UUID.fromString("0199f0a0-0000-7000-8000-000000000001");

    private final RangoRecomendacionRepositorio repositorio = mock(RangoRecomendacionRepositorio.class);
    private final RangosDeLaEmpresaJpa rangos = new RangosDeLaEmpresaJpa(repositorio);

    private static RangoRecomendacion fila(RangoRecomendacionCategoria categoria, String limite, String texto) {
        RangoRecomendacion fila = new RangoRecomendacion();
        fila.setEmpresaId(EMPRESA);
        fila.setCategoria(categoria);
        fila.setLimiteInferior(new BigDecimal(limite));
        fila.setRecomendacion(texto);
        return fila;
    }

    @Test
    @DisplayName("RF-13 · C2 · el análisis usa los rangos que la empresa cambió, con el código de cada categoría")
    void losDeLaEmpresa() {
        when(repositorio.findAllByEmpresaId(EMPRESA)).thenReturn(List.of(
                fila(RangoRecomendacionCategoria.FUERA_DE_NORMA, "0.0", "Reproceso"),
                fila(RangoRecomendacionCategoria.ESTANDAR, "91.0", "Venta de la casa"),
                fila(RangoRecomendacionCategoria.PREMIUM, "97.5", "Exportación"),
                fila(RangoRecomendacionCategoria.CORRIENTE, "80.0", "Selección a mano")));

        assertThat(rangos.vigentes(EMPRESA)).containsExactly(
                new RangoDeCategoria("PREMIUM", new BigDecimal("97.5"), "Exportación"),
                new RangoDeCategoria("ESTANDAR", new BigDecimal("91.0"), "Venta de la casa"),
                new RangoDeCategoria("CORRIENTE", new BigDecimal("80.0"), "Selección a mano"),
                new RangoDeCategoria("FUERA_DE_NORMA", new BigDecimal("0.0"), "Reproceso"));
    }

    @Test
    @DisplayName("RF-13 · si a la empresa le falta una categoría, va la de por defecto (D-10), sin guardarla")
    void completaLasQueFaltan() {
        when(repositorio.findAllByEmpresaId(EMPRESA)).thenReturn(List.of(
                fila(RangoRecomendacionCategoria.ESTANDAR, "91.0", "Venta de la casa")));
        RangoRecomendacion premium = RangoRecomendacionValoresIniciales.paraEmpresa(EMPRESA).get(0);

        List<RangoDeCategoria> vigentes = rangos.vigentes(EMPRESA);

        assertThat(vigentes).extracting(RangoDeCategoria::categoria)
                .containsExactly("PREMIUM", "ESTANDAR", "CORRIENTE", "FUERA_DE_NORMA");
        assertThat(vigentes.get(0)).isEqualTo(new RangoDeCategoria("PREMIUM", premium.getLimiteInferior(),
                premium.getRecomendacion()));
        assertThat(vigentes.get(1).limiteInferior()).isEqualTo(new BigDecimal("91.0"));
        verify(repositorio, never()).save(any());
        verify(repositorio, never()).saveAll(any());
    }
}
