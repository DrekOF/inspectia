/*
 * Nombre: ClasificadorDelServicio.java
 * Descripción: El puerto ClasificadorDeGranos con el servicio de IA: revisa y clasifica las fotos de una muestra con el
 *              modelo de Julián, por HTTP (RF-06, RF-07, RF-08, D-40, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.ia;

import co.edu.usco.inspectia.analisis.aplicacion.Clasificacion;
import co.edu.usco.inspectia.analisis.aplicacion.ClasificadorDeGranos;
import co.edu.usco.inspectia.analisis.aplicacion.FotoParaClasificar;
import java.util.List;

/**
 * El clasificador de granos con el servicio de IA ({@code inspectia.ia.clasificador=servicio}): manda las fotos a
 * {@code POST /analizar} y responde la revisión de cada foto y los granos de las que la pasan (D-46). Antes se asegura
 * de que los modelos del servicio estén registrados, por si el servicio no respondía cuando arrancó el servidor: la
 * muestra guarda el modelo que la clasificó (RF-08 C3).
 */
public class ClasificadorDelServicio implements ClasificadorDeGranos {

    private final ClienteDelServicioDeIa cliente;
    private final RegistroDeModelosDelServicio registro;

    /**
     * Crea el clasificador.
     *
     * @param cliente el cliente HTTP del servicio
     * @param registro el registro de sus modelos
     */
    public ClasificadorDelServicio(ClienteDelServicioDeIa cliente, RegistroDeModelosDelServicio registro) {
        this.cliente = cliente;
        this.registro = registro;
    }

    @Override
    public Clasificacion clasificar(List<FotoParaClasificar> fotos) {
        registro.asegurar();
        return cliente.analizar(fotos);
    }
}
