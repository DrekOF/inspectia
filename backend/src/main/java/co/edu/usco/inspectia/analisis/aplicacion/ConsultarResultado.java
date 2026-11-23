/*
 * Nombre: ConsultarResultado.java
 * Descripción: Caso de uso de consultar el resultado guardado de un lote y el detalle de sus muestras, con las fotos
 *              marcadas y sus granos, sin la confianza (RF-10, RF-11, RF-12, RF-14 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.PorcentajeDelLote;
import co.edu.usco.inspectia.analisis.dominio.ReglasDeConfianza;
import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El resultado de un lote y el detalle de sus muestras (RF-11, RF-14 C3), para el usuario final y la administradora de
 * empresa. Muestra lo guardado y no recalcula nada con los rangos ni con los parámetros de hoy (RF-13 C3): un grano es
 * dudoso con el umbral que copió su muestra (RF-09 C5). La confianza no sale de aquí (RF-11 C4, D-42).
 *
 * <p>Cada foto lleva un enlace nuevo para verla, que vence en 15 minutos (D-41). Solo responde lotes de la empresa de
 * la sesión (RNF-03).
 */
@Service
@Transactional(readOnly = true)
public class ConsultarResultado {

    private final RepositorioDeLotes lotes;
    private final RepositorioDeMuestras muestras;
    private final VistasDeFotos vistas;

    /**
     * Crea el caso de uso; Spring le pasa sus dependencias.
     *
     * @param lotes los lotes guardados
     * @param muestras las muestras, sus fotos y sus granos
     * @param vistas las vistas de las fotos, con su enlace para verlas
     */
    public ConsultarResultado(RepositorioDeLotes lotes, RepositorioDeMuestras muestras, VistasDeFotos vistas) {
        this.lotes = lotes;
        this.muestras = muestras;
        this.vistas = vistas;
    }

    /**
     * El resultado guardado del lote y el detalle de sus muestras, por número.
     *
     * @param empresaId la empresa de la sesión
     * @param loteId el lote
     * @return el resultado; con el lote pendiente, sus cifras van vacías y cada foto, sin granos
     * @throws NoEncontradoExcepcion si el lote no existe o es de otra empresa (404, RNF-03 C1)
     */
    public DetalleDelResultado detalle(UUID empresaId, UUID loteId) {
        ResultadoGuardado lote = lotes.resultado(empresaId, loteId)
                .orElseThrow(() -> new NoEncontradoExcepcion("lote " + loteId));
        List<DetalleDeMuestra> detalle = muestras.delLote(empresaId, loteId).stream()
                .map(muestra -> detalleDe(empresaId, muestra))
                .toList();
        List<MuestraGuardada> terminadas = detalle.stream().map(DetalleDeMuestra::muestra)
                .filter(muestra -> muestra.estado() == EstadoDeMuestra.TERMINADA)
                .toList();
        if (lote.estado() != EstadoDeLote.ANALIZADO || terminadas.isEmpty()) {
            return new DetalleDelResultado(lote, null, null, null, detalle);
        }
        int total = terminadas.stream().mapToInt(MuestraGuardada::totalGranos).sum();
        int sanos = terminadas.stream().mapToInt(MuestraGuardada::granosSanos).sum();
        return new DetalleDelResultado(lote, total, sanos, PorcentajeDelLote.contado(sanos, total), detalle);
    }

    private DetalleDeMuestra detalleDe(UUID empresaId, MuestraGuardada muestra) {
        List<FotoGuardada> fotos = muestras.fotos(empresaId, muestra.id());
        if (muestra.estado() != EstadoDeMuestra.TERMINADA) {
            return new DetalleDeMuestra(muestra, null,
                    fotos.stream().map(foto -> new FotoMarcada(foto.numero(), vistas.de(foto).vista(), List.of()))
                            .toList());
        }
        ReglasDeConfianza confianza = new ReglasDeConfianza(muestra.umbralConfianzaBaja(),
                muestra.porcentajeMaxDudosos());
        Map<UUID, List<GranoMarcado>> granosPorFoto = muestras
                .granos(empresaId, fotos.stream().map(FotoGuardada::id).toList()).stream()
                .collect(Collectors.groupingBy(GranoDeFoto::fotoId, LinkedHashMap::new,
                        Collectors.mapping(grano -> marcado(grano, confianza), Collectors.toList())));
        List<FotoMarcada> marcadas = fotos.stream()
                .map(foto -> new FotoMarcada(foto.numero(), vistas.de(foto).vista(),
                        granosPorFoto.getOrDefault(foto.id(), List.of())))
                .toList();
        BigDecimal contado = PorcentajeDelLote.contado(muestra.granosSanos(), muestra.totalGranos());
        return new DetalleDeMuestra(muestra, contado, marcadas);
    }

    private static GranoMarcado marcado(GranoDeFoto grano, ReglasDeConfianza confianza) {
        return new GranoMarcado(grano.x(), grano.y(), grano.ancho(), grano.alto(), grano.clase(),
                confianza.esDudoso(grano.confianza()));
    }
}
