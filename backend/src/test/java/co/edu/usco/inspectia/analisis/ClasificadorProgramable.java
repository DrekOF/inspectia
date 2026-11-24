/*
 * Nombre: ClasificadorProgramable.java
 * Descripción: El clasificador de granos de las pruebas de integración: responde como el clasificador falso, o lo que
 *              cada prueba le programa, como otro modelo, pocos granos o un fallo (RF-08, RF-10, RNF-02).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import co.edu.usco.inspectia.analisis.adaptadores.falso.ClasificadorFalso;
import co.edu.usco.inspectia.analisis.aplicacion.Clasificacion;
import co.edu.usco.inspectia.analisis.aplicacion.ClasificadorDeGranos;
import co.edu.usco.inspectia.analisis.aplicacion.FotoParaClasificar;
import co.edu.usco.inspectia.analisis.aplicacion.ModeloUsado;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * El clasificador de las pruebas de integración. Es un bean {@code @Primary} de
 * {@link co.edu.usco.inspectia.ConfiguracionContenedores}, así todas las pruebas comparten el contexto de Spring y el
 * contenedor de la base. Sin programar, responde como el {@link ClasificadorFalso}: cada prueba arma su caso con el
 * ancho y el alto de las fotos que sube. Las pruebas que lo programan lo restablecen al terminar.
 */
public final class ClasificadorProgramable implements ClasificadorDeGranos {

    private final ClasificadorFalso falso = new ClasificadorFalso(Duration.ZERO);
    private final AtomicReference<Function<List<FotoParaClasificar>, Clasificacion>> respuesta =
            new AtomicReference<>();

    @Override
    public Clasificacion clasificar(List<FotoParaClasificar> fotos) {
        Function<List<FotoParaClasificar>, Clasificacion> programada = respuesta.get();
        return programada == null ? falso.clasificar(fotos) : programada.apply(fotos);
    }

    /**
     * Responde los granos del clasificador falso, pero como otro modelo, que la prueba registró en la tabla
     * {@code modelo}.
     *
     * @param modelo el modelo con que responde
     */
    public void conModelo(ModeloUsado modelo) {
        respuesta.set(fotos -> new Clasificacion(modelo, falso.clasificar(fotos).granos()));
    }

    /**
     * Responde solo los primeros granos del clasificador falso.
     *
     * @param cuantos cuántos granos responde
     */
    public void conGranos(int cuantos) {
        respuesta.set(fotos -> new Clasificacion(ClasificadorFalso.MODELO,
                falso.clasificar(fotos).granos().subList(0, cuantos)));
    }

    /**
     * Falla al clasificar, como un servicio de IA que no responde.
     *
     * @param error el error con que falla
     */
    public void fallar(RuntimeException error) {
        respuesta.set(fotos -> {
            throw error;
        });
    }

    /** Vuelve a responder como el clasificador falso. */
    public void restablecer() {
        respuesta.set(null);
    }
}
