/*
 * Nombre: VistasDeFotos.java
 * Descripción: Arma la vista de una foto guardada, con un enlace nuevo para verla que vence en 15 minutos (RF-05,
 *              RNF-03 C3, D-41).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Las vistas de las fotos guardadas (RF-05). Cada una lleva un enlace de vista nuevo: solo lo recibe la empresa
 * dueña, que pidió la foto con su sesión, y vence en minutos (RNF-03 C3).
 */
@Component
public class VistasDeFotos {

    /** Cuánto dura un enlace para ver una foto (D-41). */
    public static final Duration VIGENCIA_DE_VISTA = Duration.ofMinutes(15);

    private final AlmacenDeArchivos almacen;

    /**
     * Crea las vistas; Spring le pasa el almacén.
     *
     * @param almacen el almacén de los archivos
     */
    public VistasDeFotos(AlmacenDeArchivos almacen) {
        this.almacen = almacen;
    }

    /**
     * La vista de una foto, con un enlace nuevo para verla.
     *
     * @param foto la foto guardada
     * @return la vista
     */
    public VistaDeFoto de(FotoGuardada foto) {
        return new VistaDeFoto(foto.numero(), foto.formato(), foto.tamanoBytes(), foto.estado(),
                almacen.enlaceDeVista(foto.llave(), VIGENCIA_DE_VISTA));
    }
}
