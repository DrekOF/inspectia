/*
 * Nombre: VistasDeFotosRevisadas.java
 * Descripción: Las vistas de las fotos de una muestra con lo que dijo su revisión: su motivo, sus granos encontrados,
 *              sus regiones de granos pegados y, en la foto que RF-09 pidió repetir, sus granos dudosos (RF-06, RF-07
 *              C2, RF-09 C1, D-45, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeFoto;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Arma las vistas de las fotos de una muestra para la página (RF-05, D-46): cada una con su enlace nuevo y lo que dijo
 * su revisión. Las regiones de granos pegados solo se leen para las fotos con ese motivo (D-45), y los granos dudosos,
 * sin clase ni confianza (RF-11 C4), solo para la foto que RF-09 pidió repetir, con el umbral que copió su muestra
 * (RF-09 C5). Las demás fotos no guardan granos hasta que la muestra termina.
 */
@Component
public class VistasDeFotosRevisadas {

    private final RepositorioDeMuestras muestras;
    private final VistasDeFotos vistas;

    /**
     * Crea las vistas; Spring le pasa sus dependencias.
     *
     * @param muestras las muestras, sus fotos, sus regiones y sus granos
     * @param vistas las vistas de las fotos, con su enlace para verlas
     */
    public VistasDeFotosRevisadas(RepositorioDeMuestras muestras, VistasDeFotos vistas) {
        this.muestras = muestras;
        this.vistas = vistas;
    }

    /**
     * Las vistas de las fotos de una muestra.
     *
     * @param empresaId la empresa
     * @param muestra la muestra, con la copia del umbral si RF-09 la evaluó
     * @param fotos sus fotos, por número
     * @return las vistas, en el mismo orden
     */
    public List<VistaDeFoto> de(UUID empresaId, MuestraEnCurso muestra, List<FotoGuardada> fotos) {
        Map<UUID, List<Rectangulo>> regiones = conMotivo(fotos, MotivoDeFoto.GRANOS_PEGADOS).isEmpty() ? Map.of()
                : muestras.regionesPegadas(empresaId, conMotivo(fotos, MotivoDeFoto.GRANOS_PEGADOS));
        Map<UUID, List<Rectangulo>> dudosos = dudosos(empresaId, muestra.umbralConfianzaBaja(),
                conMotivo(fotos, MotivoDeFoto.GRANOS_DUDOSOS));
        return fotos.stream().map(foto -> {
            VistaDeFoto vista = vistas.de(foto);
            return new VistaDeFoto(vista.numero(), vista.formato(), vista.tamanoBytes(), vista.estado(), vista.vista(),
                    foto.motivo(), foto.granosEncontrados(), regiones.getOrDefault(foto.id(), List.of()),
                    dudosos.getOrDefault(foto.id(), List.of()));
        }).toList();
    }

    /**
     * Las fotos por repetir de una muestra, por número (D-44).
     *
     * @param fotos sus fotos
     * @return las que están por repetir, con su motivo
     */
    public static List<FotoPorRepetir> porRepetir(List<FotoGuardada> fotos) {
        return fotos.stream()
                .filter(foto -> foto.estado() == EstadoDeFoto.POR_REPETIR && foto.motivo() != null)
                .map(foto -> new FotoPorRepetir(foto.numero(), foto.motivo()))
                .toList();
    }

    private static List<UUID> conMotivo(Collection<FotoGuardada> fotos, MotivoDeFoto motivo) {
        return fotos.stream().filter(foto -> foto.estado() == EstadoDeFoto.POR_REPETIR && foto.motivo() == motivo)
                .map(FotoGuardada::id).toList();
    }

    /** Los granos con confianza menor que el umbral copiado: los dudosos (P-15, RF-09 C5). */
    private Map<UUID, List<Rectangulo>> dudosos(UUID empresaId, BigDecimal umbral, List<UUID> fotos) {
        if (fotos.isEmpty() || umbral == null) {
            return Map.of();
        }
        return muestras.granos(empresaId, fotos).stream()
                .filter(grano -> grano.confianza().compareTo(umbral) < 0)
                .collect(Collectors.groupingBy(GranoDeFoto::fotoId, Collectors.mapping(
                        grano -> new Rectangulo(grano.x(), grano.y(), grano.ancho(), grano.alto()),
                        Collectors.toList())));
    }
}
