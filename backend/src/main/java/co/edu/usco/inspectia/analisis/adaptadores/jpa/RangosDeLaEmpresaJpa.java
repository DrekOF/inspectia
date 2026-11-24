/*
 * Nombre: RangosDeLaEmpresaJpa.java
 * Descripción: El puerto RangosDeLaEmpresa con el repositorio generado de rango_recomendacion: los rangos vigentes de
 *              una empresa, con los de por defecto en las categorías que le falten, sin guardarlos (RF-12, RF-13).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.aplicacion.RangosDeLaEmpresa;
import co.edu.usco.inspectia.analisis.dominio.RangoDeCategoria;
import co.edu.usco.inspectia.rango.RangoRecomendacion;
import co.edu.usco.inspectia.rango.RangoRecomendacionCategoria;
import co.edu.usco.inspectia.rango.RangoRecomendacionRepositorio;
import co.edu.usco.inspectia.rango.RangoRecomendacionValoresIniciales;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Los rangos vigentes de una empresa (RF-13), desde el CRUD generado de {@code rango_recomendacion}: lo escrito a mano
 * puede usar lo generado, y no al revés (RP-23). Cada empresa tiene siempre sus 4 categorías; si le falta alguna, como
 * en una base cargada a mano, va la de por defecto (D-10), igual que al editar los rangos, pero sin guardarla. Los
 * rangos salen con el código de su categoría, no con el enum generado ({@link RangoDeCategoria}).
 */
@Component
public class RangosDeLaEmpresaJpa implements RangosDeLaEmpresa {

    private final RangoRecomendacionRepositorio rangos;

    /**
     * Crea el adaptador; Spring le pasa el repositorio generado.
     *
     * @param rangos la tabla {@code rango_recomendacion}
     */
    public RangosDeLaEmpresaJpa(RangoRecomendacionRepositorio rangos) {
        this.rangos = rangos;
    }

    @Override
    public List<RangoDeCategoria> vigentes(UUID empresaId) {
        Map<RangoRecomendacionCategoria, RangoRecomendacion> guardados = new EnumMap<>(RangoRecomendacionCategoria.class);
        rangos.findAllByEmpresaId(empresaId).forEach(fila -> guardados.putIfAbsent(fila.getCategoria(), fila));
        return RangoRecomendacionValoresIniciales.paraEmpresa(empresaId).stream()
                .map(porDefecto -> guardados.getOrDefault(porDefecto.getCategoria(), porDefecto))
                .map(fila -> new RangoDeCategoria(fila.getCategoria().name(), fila.getLimiteInferior(),
                        fila.getRecomendacion()))
                .toList();
    }
}
