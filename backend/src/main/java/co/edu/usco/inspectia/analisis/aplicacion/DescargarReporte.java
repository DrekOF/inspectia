/*
 * Nombre: DescargarReporte.java
 * Descripción: Caso de uso de descargar el reporte PDF de un lote analizado: la primera vez lo arma en español y en
 *              inglés y lo guarda en el almacén; después entrega el guardado, aunque la empresa cambie sus rangos
 *              (RF-15, D-25, D-50).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.PorcentajeDelLote;
import co.edu.usco.inspectia.analisis.dominio.ReglasDeConfianza;
import co.edu.usco.inspectia.comun.errores.ConflictoExcepcion;
import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El reporte PDF de un lote (RF-15), para el usuario final y la administradora de empresa. Solo de los lotes de la
 * empresa de la sesión (RNF-03) y analizados.
 *
 * <p>La primera descarga bloquea el lote, como {@link GuardarAnalisis}, arma el reporte con lo guardado en los dos
 * idiomas (D-50), escribe sus dos archivos en el almacén y guarda su base en el lote. Las siguientes entregan el archivo
 * guardado: así se obtiene el mismo reporte aunque la empresa cambie sus rangos o sus nombres (RF-15 C4). Si falta el
 * archivo del idioma pedido, lo vuelve a armar con una base nueva y descarta la anterior.
 *
 * <p>Cuando termina la muestra 2, {@link GuardarAnalisis} descarta el reporte de la 1 y la siguiente descarga lo rehace
 * con el resultado de las dos (RF-15 C5, D-25).
 */
@Slf4j
@Service
public class DescargarReporte {

    /** El tipo de los archivos del reporte. */
    public static final String TIPO = "application/pdf";

    /** Lo que no va en el nombre del archivo: lo que no deja guardarlo en Windows, los espacios y los de control. */
    private static final Pattern NO_VA_EN_EL_NOMBRE = Pattern.compile("[\\\\/:*?\"<>|\\s\\p{Cntrl}]");

    private final RepositorioDeLotes lotes;
    private final RepositorioDeMuestras muestras;
    private final AlmacenDeArchivos almacen;
    private final GeneradorDeReporte generador;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    /**
     * Crea el caso de uso; Spring le pasa sus dependencias.
     *
     * @param lotes los lotes guardados
     * @param muestras las muestras, sus fotos y sus granos
     * @param almacen el almacén de los archivos: las fotos y los reportes
     * @param generador el que arma el PDF
     * @param eventos para borrar los archivos de un reporte descartado, o del nuevo si la transacción se deshace
     * @param reloj el reloj de la aplicación, para la fecha del reporte
     */
    public DescargarReporte(RepositorioDeLotes lotes, RepositorioDeMuestras muestras, AlmacenDeArchivos almacen,
            GeneradorDeReporte generador, ApplicationEventPublisher eventos, Clock reloj) {
        this.lotes = lotes;
        this.muestras = muestras;
        this.almacen = almacen;
        this.generador = generador;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /**
     * La base de los archivos de un reporte (D-50).
     *
     * @param empresaId la empresa
     * @param loteId el lote
     * @param id el identificador del reporte
     * @return {@code reportes/<empresa>/<lote>/<id>}
     */
    public static String base(UUID empresaId, UUID loteId, UUID id) {
        return "reportes/" + empresaId + "/" + loteId + "/" + id;
    }

    /**
     * La llave del archivo de un reporte en un idioma (D-50).
     *
     * @param base la base del reporte
     * @param idioma el idioma
     * @return {@code <base>-<es|en>.pdf}
     */
    public static String llave(String base, IdiomaDelReporte idioma) {
        return base + "-" + idioma.codigo() + ".pdf";
    }

    /**
     * El nombre del archivo que descarga el navegador, como {@code InspectIA-L-0143-es.pdf}. Lo que no se puede
     * escribir en un nombre de archivo, como {@code /} o un espacio, va como {@code _}.
     *
     * @param codigo el código del lote
     * @param idioma el idioma del reporte
     * @return el nombre
     */
    public static String nombreDelArchivo(String codigo, IdiomaDelReporte idioma) {
        return "InspectIA-" + NO_VA_EN_EL_NOMBRE.matcher(codigo.strip()).replaceAll("_") + "-" + idioma.codigo()
                + ".pdf";
    }

    /**
     * El reporte PDF de un lote en un idioma.
     *
     * @param empresaId la empresa de la sesión
     * @param loteId el lote
     * @param idioma el idioma del reporte
     * @return el nombre del archivo y sus bytes
     * @throws NoEncontradoExcepcion si el lote no existe o es de otra empresa (404, RNF-03)
     * @throws ConflictoExcepcion si el lote está pendiente de análisis (409)
     */
    @Transactional
    public ReporteDelLote descargar(UUID empresaId, UUID loteId, IdiomaDelReporte idioma) {
        EstadoDeLote estado = lotes.estadoParaActualizar(empresaId, loteId)
                .orElseThrow(() -> new NoEncontradoExcepcion("lote " + loteId));
        if (estado != EstadoDeLote.ANALIZADO) {
            throw new ConflictoExcepcion("reporte.lote_pendiente");
        }
        ResultadoGuardado lote = lotes.resultado(empresaId, loteId).orElseThrow();
        DatosDelReporte datos = lotes.datosDelReporte(empresaId, loteId).orElseThrow();
        String nombre = nombreDelArchivo(lote.codigo(), idioma);
        if (datos.rutaDelReporte() != null) {
            String guardado = llave(datos.rutaDelReporte(), idioma);
            if (almacen.describir(guardado).isPresent()) {
                return new ReporteDelLote(nombre, almacen.leer(guardado));
            }
            log.warn("Falta el archivo {} del reporte del lote {}: se arma de nuevo", guardado, loteId);
            eventos.publishEvent(new ReporteDescartado(datos.rutaDelReporte()));
        }
        ContenidoDelReporte contenido = contenido(empresaId, lote, datos);
        String base = base(empresaId, loteId, UUID.randomUUID());
        eventos.publishEvent(new ReporteNuevo(base));
        byte[] pedido = null;
        for (IdiomaDelReporte cada : IdiomaDelReporte.values()) {
            byte[] pdf = generador.generar(contenido, cada);
            almacen.guardar(llave(base, cada), pdf, TIPO);
            if (cada == idioma) {
                pedido = pdf;
            }
        }
        lotes.guardarRutaDelReporte(empresaId, loteId, base);
        return new ReporteDelLote(nombre, pedido);
    }

    /** Lo que lleva el reporte: el lote, su empresa y sus muestras terminadas, con los granos de la suma (D-25). */
    private ContenidoDelReporte contenido(UUID empresaId, ResultadoGuardado lote, DatosDelReporte datos) {
        List<ContenidoDelReporte.MuestraDelReporte> terminadas = new ArrayList<>();
        int total = 0;
        int sanos = 0;
        for (MuestraGuardada muestra : muestras.delLote(empresaId, lote.id())) {
            if (muestra.estado() != EstadoDeMuestra.TERMINADA) {
                continue;
            }
            total += muestra.totalGranos();
            sanos += muestra.granosSanos();
            terminadas.add(new ContenidoDelReporte.MuestraDelReporte(muestra,
                    PorcentajeDelLote.contado(muestra.granosSanos(), muestra.totalGranos()),
                    fotos(empresaId, muestra)));
        }
        return new ContenidoDelReporte(datos.empresa(), datos.nit(), lote, total, sanos,
                PorcentajeDelLote.contado(sanos, total), terminadas, reloj.instant());
    }

    /** Las fotos de una muestra terminada, con sus granos marcados con el umbral que copió la muestra (RF-09 C5). */
    private List<ContenidoDelReporte.FotoDelReporte> fotos(UUID empresaId, MuestraGuardada muestra) {
        List<FotoGuardada> fotos = muestras.fotos(empresaId, muestra.id());
        ReglasDeConfianza confianza = new ReglasDeConfianza(muestra.umbralConfianzaBaja(),
                muestra.porcentajeMaxDudosos());
        Map<UUID, List<GranoMarcado>> granosPorFoto = muestras
                .granos(empresaId, fotos.stream().map(FotoGuardada::id).toList()).stream()
                .collect(Collectors.groupingBy(GranoDeFoto::fotoId, LinkedHashMap::new,
                        Collectors.mapping(grano -> new GranoMarcado(grano.x(), grano.y(), grano.ancho(),
                                grano.alto(), grano.clase(), confianza.esDudoso(grano.confianza())),
                                Collectors.toList())));
        return fotos.stream()
                .map(foto -> new ContenidoDelReporte.FotoDelReporte(foto.numero(), leer(foto.llave()),
                        granosPorFoto.getOrDefault(foto.id(), List.of())))
                .toList();
    }

    /** Los bytes de una foto, o {@code null} si el almacén no la tiene: el reporte sale igual, sin esa foto. */
    private byte[] leer(String llave) {
        try {
            return almacen.leer(llave);
        } catch (RuntimeException excepcion) {
            log.warn("No se pudo leer la foto {} para el reporte", llave, excepcion);
            return null;
        }
    }
}
