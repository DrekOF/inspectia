/*
 * Nombre: GeneradorDeReporteEnPdf.java
 * Descripción: El puerto GeneradorDeReporte con Apache PDFBox: arma el reporte PDF de un lote en español o en inglés,
 *              con los datos del lote y de la empresa, el porcentaje corregido con su rango probable, lo que contó el
 *              modelo, la categoría probable con la nota de la prueba de corte, la recomendación, la sugerencia con su
 *              límite, el aviso de confianza baja y cada muestra con su modelo y sus fotos marcadas (RF-15, D-10,
 *              D-18, D-25, D-39, D-42, D-43, D-47).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.pdf;

import co.edu.usco.inspectia.analisis.aplicacion.ContenidoDelReporte;
import co.edu.usco.inspectia.analisis.aplicacion.ContenidoDelReporte.FotoDelReporte;
import co.edu.usco.inspectia.analisis.aplicacion.ContenidoDelReporte.MuestraDelReporte;
import co.edu.usco.inspectia.analisis.aplicacion.GeneradorDeReporte;
import co.edu.usco.inspectia.analisis.aplicacion.GranoMarcado;
import co.edu.usco.inspectia.analisis.aplicacion.IdiomaDelReporte;
import co.edu.usco.inspectia.analisis.aplicacion.MuestraGuardada;
import co.edu.usco.inspectia.analisis.aplicacion.ResultadoGuardado;
import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * El reporte PDF de un lote con Apache PDFBox (RF-15). Apache PDFBox tiene licencia Apache 2.0, como Spring, y lee el
 * texto de los PDF que escribe, con lo que las pruebas comparan las cifras con las de la página (RF-15 C2).
 *
 * <p>El diseño sigue el del prototipo, que no dibuja el reporte: la barra café, la cifra grande con el rango probable,
 * los conteos en verde y rojo con su símbolo, la categoría en su chip, la sugerencia en ámbar y las fotos con los
 * granos marcados como en la página: verde con chulo si es sano, rojo con equis si no y una marca «?» si es dudoso
 * (RF-11, D-42). Todo se dibuja con vectores sobre la foto, sin fuentes del sistema: el servidor no las tiene.
 *
 * <p>Solo escribe lo guardado: el porcentaje, el margen, la categoría, la recomendación y el límite de la sugerencia
 * son los del lote, sin recalcularlos con los rangos de hoy (RF-15 C4).
 */
@Component
public class GeneradorDeReporteEnPdf implements GeneradorDeReporte {

    /** Separación entre las dos columnas de las fotos y del resumen. */
    private static final float SEPARACION = 16;

    /** Relleno de los recuadros. */
    private static final float RELLENO = 18;

    /** Radio de las esquinas de los recuadros, como las tarjetas de la página. */
    private static final float RADIO = 10;

    private final MessageSource mensajes;

    /**
     * Crea el generador; Spring le pasa los textos del backend.
     *
     * @param mensajes los textos, en {@code i18n/mensajes} e {@code i18n/generado}
     */
    public GeneradorDeReporteEnPdf(MessageSource mensajes) {
        this.mensajes = mensajes;
    }

    @Override
    public byte[] generar(ContenidoDelReporte contenido, IdiomaDelReporte idioma) {
        FormatoDelReporte formato = new FormatoDelReporte(idioma, mensajes);
        ResultadoGuardado lote = contenido.lote();
        try (HojaDelReporte hoja = new HojaDelReporte(formato.texto("reporte.titulo", lote.codigo()),
                formato.texto("reporte.asunto", lote.codigo(), contenido.empresa()))) {
            hoja.idioma(idioma.region().toLanguageTag());
            encabezado(hoja, formato, contenido);
            datosDelLote(hoja, formato, contenido);
            resumen(hoja, formato, contenido);
            sugerencia(hoja, formato, lote);
            confianzaBaja(hoja, formato, lote);
            for (MuestraDelReporte muestra : contenido.muestras()) {
                muestra(hoja, formato, muestra);
            }
            Instant generado = contenido.generado();
            return hoja.terminar(formato.texto("reporte.pie", formato.dia(generado), formato.hora(generado)),
                    formato.texto("reporte.pagina", "{0}", "{1}"));
        }
    }

    /** El título y la línea de debajo, como en la página: la finca, la recepción y cuándo se analizó. */
    private static void encabezado(HojaDelReporte hoja, FormatoDelReporte formato, ContenidoDelReporte contenido) {
        ResultadoGuardado lote = contenido.lote();
        hoja.parrafo(formato.texto("reporte.resultado", lote.codigo()), hoja.media(), 22, HojaDelReporte.TEXTO);
        hoja.bajar(2);
        Instant analizado = lote.fechaResultado().toInstant();
        hoja.parrafo(formato.texto("reporte.datos", lote.origen(), formato.dia(lote.fechaRecepcion()),
                formato.dia(analizado), formato.hora(analizado)), hoja.normal(), 11, HojaDelReporte.SECUNDARIO);
        hoja.bajar(14);
    }

    /** Los datos del lote y de su empresa, en dos columnas de etiqueta y valor. */
    private static void datosDelLote(HojaDelReporte hoja, FormatoDelReporte formato, ContenidoDelReporte contenido) {
        ResultadoGuardado lote = contenido.lote();
        Instant analizado = lote.fechaResultado().toInstant();
        String[][] datos = {
            {formato.texto("reporte.empresa"), contenido.empresa()},
            {formato.texto("reporte.nit"), contenido.nit()},
            {formato.texto("reporte.codigo"), lote.codigo()},
            {formato.texto("reporte.origen"), lote.origen()},
            {formato.texto("reporte.fecha_recepcion"), formato.dia(lote.fechaRecepcion())},
            {formato.texto("reporte.fecha_resultado"),
                formato.texto("reporte.fecha_y_hora", formato.dia(analizado), formato.hora(analizado))},
        };
        float columna = (hoja.ancho() - 2 * RELLENO - SEPARACION) / 2;
        List<Float> altos = new ArrayList<>();
        float alto = 0;
        for (int i = 0; i < datos.length; i += 2) {
            float fila = Math.max(altoDelDato(hoja, datos[i], columna), altoDelDato(hoja, datos[i + 1], columna));
            altos.add(fila);
            alto += fila;
        }
        alto += 2 * RELLENO - 10 + hoja.altoDe(formato.texto("reporte.datos_del_lote"), hoja.media(), 13,
                hoja.ancho()) + 6;
        hoja.asegurar(alto);
        float arriba = hoja.y();
        hoja.rectangulo(HojaDelReporte.MARGEN, arriba - alto, hoja.ancho(), alto, HojaDelReporte.FONDO, null, RADIO);
        float x = HojaDelReporte.MARGEN + RELLENO;
        float linea = arriba - RELLENO;
        linea -= hoja.parrafo(formato.texto("reporte.datos_del_lote"), hoja.media(), 13, HojaDelReporte.TEXTO, x,
                linea, hoja.ancho() - 2 * RELLENO) + 6;
        for (int i = 0; i < datos.length; i += 2) {
            dato(hoja, datos[i], x, linea, columna);
            dato(hoja, datos[i + 1], x + columna + SEPARACION, linea, columna);
            linea -= altos.get(i / 2);
        }
        hoja.bajar(alto + 16);
    }

    private static float altoDelDato(HojaDelReporte hoja, String[] dato, float ancho) {
        return hoja.altoDe(dato[0], hoja.media(), 9, ancho) + hoja.altoDe(dato[1], hoja.normal(), 11, ancho) + 8;
    }

    private static void dato(HojaDelReporte hoja, String[] dato, float x, float arriba, float ancho) {
        float alto = hoja.parrafo(dato[0], hoja.media(), 9, HojaDelReporte.SECUNDARIO, x, arriba, ancho);
        hoja.parrafo(dato[1], hoja.normal(), 11, HojaDelReporte.TEXTO, x, arriba - alto, ancho);
    }

    /**
     * El resumen del resultado, como la tarjeta de la página: a la izquierda la cifra, el rango probable, los conteos y
     * lo que contó el modelo con la frase que explica la diferencia (D-39, D-43); a la derecha la categoría probable,
     * la recomendación guardada y la nota de la prueba de corte (D-10).
     */
    private static void resumen(HojaDelReporte hoja, FormatoDelReporte formato, ContenidoDelReporte contenido) {
        ResultadoGuardado lote = contenido.lote();
        float ancho = hoja.ancho();
        float columna = (ancho - 2 * RELLENO - 2 * SEPARACION) / 2;
        String[] rango = formato.rango(lote.porcentajeSanos(), lote.margen());
        String textoDelRango = formato.texto("reporte.rango", rango[0], rango[1]);
        String marcados = formato.texto("reporte.marcados", contenido.granosSanos(), contenido.totalGranos());
        String contado = formato.texto("reporte.contado", formato.porcentaje(contenido.porcentajeContado()));
        String corrige = formato.texto("reporte.corrige");
        String categoria = formato.categoria(lote.categoria());
        String nota = formato.texto("reporte.nota");
        String recomendacion = lote.recomendacion();
        float izquierda = 44 + 18 + hoja.altoDe(textoDelRango, hoja.normal(), 11, columna) + 8 + 50 + 10
                + hoja.altoDe(marcados, hoja.normal(), 10, columna) + hoja.altoDe(contado, hoja.normal(), 10, columna)
                + hoja.altoDe(corrige, hoja.normal(), 9, columna);
        float derecha = 16 + 32 + 10 + hoja.altoDe(formato.texto("reporte.recomendacion"), hoja.media(), 9, columna)
                + hoja.altoDe(recomendacion, hoja.normal(), 11, columna) + 8
                + hoja.altoDe(nota, hoja.normal(), 9, columna);
        float alto = Math.max(izquierda, derecha) + 2 * RELLENO;
        hoja.asegurar(alto);
        float arriba = hoja.y();
        hoja.rectangulo(HojaDelReporte.MARGEN, arriba - alto, ancho, alto, Color.WHITE, HojaDelReporte.BORDE, RADIO);

        float x = HojaDelReporte.MARGEN + RELLENO;
        float y = arriba - RELLENO;
        hoja.texto(formato.porcentaje(lote.porcentajeSanos()), hoja.negrita(), 40, HojaDelReporte.TEXTO, x, y - 34);
        y -= 44;
        hoja.texto(formato.texto("reporte.de_granos_sanos"), hoja.normal(), 12, HojaDelReporte.SECUNDARIO, x, y - 12);
        y -= 18;
        y -= hoja.parrafo(textoDelRango, hoja.normal(), 11, HojaDelReporte.TEXTO, x, y, columna) + 8;
        float casilla = (columna - 2 * 8) / 3;
        conteo(hoja, formato.texto("reporte.granos"), contenido.totalGranos(), null, x, y, casilla);
        conteo(hoja, formato.texto("reporte.sanos"), contenido.granosSanos(), ClaseDeGrano.SANO, x + casilla + 8, y,
                casilla);
        conteo(hoja, formato.texto("reporte.no_sanos"), contenido.totalGranos() - contenido.granosSanos(),
                ClaseDeGrano.NO_SANO, x + 2 * (casilla + 8), y, casilla);
        y -= 60;
        y -= hoja.parrafo(marcados, hoja.normal(), 10, HojaDelReporte.TEXTO, x, y, columna);
        y -= hoja.parrafo(contado, hoja.normal(), 10, HojaDelReporte.TEXTO, x, y, columna);
        hoja.parrafo(corrige, hoja.normal(), 9, HojaDelReporte.SECUNDARIO, x, y, columna);

        float x2 = x + columna + 2 * SEPARACION;
        hoja.linea(HojaDelReporte.BORDE, 0.75f, x2 - SEPARACION, arriba - RELLENO, x2 - SEPARACION,
                arriba - alto + RELLENO);
        y = arriba - RELLENO;
        hoja.texto(formato.texto("reporte.categoria"), hoja.media(), 10, HojaDelReporte.SECUNDARIO, x2, y - 10);
        y -= 16;
        float anchoDelChip = Math.min(columna, hoja.anchoDe(categoria, hoja.negrita(), 15) + 28);
        hoja.rectangulo(x2, y - 28, anchoDelChip, 28, HojaDelReporte.SELECCION, null, 14);
        hoja.texto(categoria, hoja.negrita(), 15, HojaDelReporte.BARRA, x2 + 14, y - 19);
        y -= 32 + 10;
        y -= hoja.parrafo(formato.texto("reporte.recomendacion"), hoja.media(), 9, HojaDelReporte.SECUNDARIO, x2, y,
                columna);
        y -= hoja.parrafo(recomendacion, hoja.normal(), 11, HojaDelReporte.TEXTO, x2, y, columna) + 8;
        hoja.parrafo(nota, hoja.normal(), 9, HojaDelReporte.SECUNDARIO, x2, y, columna);
        hoja.bajar(alto + 14);
    }

    /** Una casilla de los conteos: el nombre con su símbolo arriba y el número debajo, en el tono de su clase. */
    private static void conteo(HojaDelReporte hoja, String nombre, int valor, ClaseDeGrano clase, float x, float arriba,
            float ancho) {
        Color fondo = clase == null ? HojaDelReporte.FONDO
                : clase == ClaseDeGrano.SANO ? HojaDelReporte.EXITO_FONDO : HojaDelReporte.ERROR_FONDO;
        Color texto = clase == null ? HojaDelReporte.SECUNDARIO
                : clase == ClaseDeGrano.SANO ? HojaDelReporte.EXITO : HojaDelReporte.ERROR_TEXTO;
        hoja.rectangulo(x, arriba - 50, ancho, 50, fondo, null, 6);
        float inicio = x + 8;
        if (clase != null) {
            simbolo(hoja, clase, inicio, arriba - 17, 9);
            inicio += 12;
        }
        hoja.texto(nombre, hoja.normal(), 9, texto, inicio, arriba - 16);
        hoja.texto(String.valueOf(valor), hoja.media(), 17, clase == null ? HojaDelReporte.TEXTO : texto, x + 8,
                arriba - 40);
    }

    /** La sugerencia de otra muestra, con el límite guardado, como el aviso de la página (D-24, D-47). */
    private static void sugerencia(HojaDelReporte hoja, FormatoDelReporte formato, ResultadoGuardado lote) {
        if (!Boolean.TRUE.equals(lote.sugerirOtraMuestra()) || lote.limiteSugerencia() == null) {
            return;
        }
        String categoria = formato.categoria(lote.categoriaDelLimite());
        String limite = formato.limite(lote.limiteSugerencia());
        String texto = lote.porcentajeSanos().compareTo(lote.limiteSugerencia()) == 0
                ? formato.texto("reporte.sugerencia_en_el_limite", categoria, limite)
                : formato.texto("reporte.sugerencia", formato.puntos(lote.porcentajeSanos(), lote.limiteSugerencia()),
                        categoria, limite);
        aviso(hoja, formato.texto("reporte.sugerencia.titulo"), texto, formato.texto("reporte.sugerencia.detalle"),
                HojaDelReporte.ADVERTENCIA, HojaDelReporte.ADVERTENCIA_TEXTO, HojaDelReporte.ADVERTENCIA_FONDO);
    }

    /** El aviso de confianza baja, si alguna muestra lo tiene (RF-09, D-25). */
    private static void confianzaBaja(HojaDelReporte hoja, FormatoDelReporte formato, ResultadoGuardado lote) {
        if (!Boolean.TRUE.equals(lote.avisoConfianzaBaja())) {
            return;
        }
        aviso(hoja, formato.texto("reporte.confianza_baja.titulo"), formato.texto("reporte.confianza_baja"), null,
                HojaDelReporte.ADVERTENCIA, HojaDelReporte.ADVERTENCIA_TEXTO, HojaDelReporte.ADVERTENCIA_FONDO);
    }

    /** Un aviso a todo el ancho, con borde y fondo de su tono: su título y uno o dos textos. */
    private static void aviso(HojaDelReporte hoja, String titulo, String texto, String detalle, Color borde,
            Color color, Color fondo) {
        float ancho = hoja.ancho() - 2 * 14;
        float alto = 2 * 12 + hoja.altoDe(titulo, hoja.media(), 11, ancho) + hoja.altoDe(texto, hoja.normal(), 10,
                ancho) + (detalle == null ? 0 : hoja.altoDe(detalle, hoja.normal(), 10, ancho));
        hoja.asegurar(alto);
        float arriba = hoja.y();
        hoja.rectangulo(HojaDelReporte.MARGEN, arriba - alto, hoja.ancho(), alto, fondo, borde, RADIO);
        float x = HojaDelReporte.MARGEN + 14;
        float y = arriba - 12;
        y -= hoja.parrafo(titulo, hoja.media(), 11, color, x, y, ancho);
        y -= hoja.parrafo(texto, hoja.normal(), 10, color, x, y, ancho);
        if (detalle != null) {
            hoja.parrafo(detalle, hoja.normal(), 10, color, x, y, ancho);
        }
        hoja.bajar(alto + 12);
    }

    /**
     * Una muestra: su número, quién la analizó, el modelo que la analizó (D-18), cuándo terminó, sus granos, lo que
     * contó el modelo con la frase de RF-11 y sus fotos marcadas, de a dos por fila, con la leyenda de las marcas.
     */
    private static void muestra(HojaDelReporte hoja, FormatoDelReporte formato, MuestraDelReporte detalle) {
        MuestraGuardada muestra = detalle.muestra();
        hoja.asegurar(140);
        hoja.bajar(6);
        hoja.parrafo(formato.texto("reporte.muestra", muestra.numero()), hoja.media(), 16, HojaDelReporte.TEXTO);
        hoja.bajar(2);
        List<String> datos = new ArrayList<>();
        datos.add(formato.texto("reporte.analizada_por", muestra.usuarioNombre()));
        datos.add(formato.texto("reporte.modelo", muestra.modeloNombre(), muestra.modeloVersion()));
        if (muestra.fechaFin() != null) {
            datos.add(formato.texto("reporte.terminada", formato.dia(muestra.fechaFin()),
                    formato.hora(muestra.fechaFin())));
        }
        hoja.parrafo(String.join(" · ", datos), hoja.normal(), 10, HojaDelReporte.SECUNDARIO);
        hoja.bajar(4);
        hoja.parrafo(formato.texto("reporte.granos_de_la_muestra", muestra.totalGranos(), muestra.granosSanos(),
                muestra.totalGranos() - muestra.granosSanos(), formato.porcentaje(detalle.porcentajeContado())),
                hoja.normal(), 10, HojaDelReporte.TEXTO);
        hoja.parrafo(formato.texto("reporte.marcados", muestra.granosSanos(), muestra.totalGranos()) + ". "
                + formato.texto("reporte.corrige"), hoja.normal(), 10, HojaDelReporte.TEXTO);
        hoja.bajar(8);
        fotos(hoja, formato, detalle.fotos());
        leyenda(hoja, formato);
        hoja.bajar(10);
    }

    /** Las fotos de una muestra, de a dos por fila, cada una con su título encima. */
    private static void fotos(HojaDelReporte hoja, FormatoDelReporte formato, List<FotoDelReporte> fotos) {
        float columna = (hoja.ancho() - SEPARACION) / 2;
        for (int i = 0; i < fotos.size(); i += 2) {
            List<FotoDelReporte> fila = fotos.subList(i, Math.min(i + 2, fotos.size()));
            List<Optional<ImagenDeLaFoto>> imagenes = fila.stream()
                    .map(foto -> ImagenDeLaFoto.leer(foto.contenido())).toList();
            float altoDeLaFila = 0;
            for (Optional<ImagenDeLaFoto> imagen : imagenes) {
                altoDeLaFila = Math.max(altoDeLaFila, imagen.map(cual -> columna * cual.altoOriginal()
                        / cual.anchoOriginal()).orElse(60f));
            }
            altoDeLaFila = Math.min(altoDeLaFila, 360);
            hoja.asegurar(altoDeLaFila + 20);
            float arriba = hoja.y();
            for (int j = 0; j < fila.size(); j++) {
                float x = HojaDelReporte.MARGEN + j * (columna + SEPARACION);
                foto(hoja, formato, fila.get(j), imagenes.get(j), x, arriba, columna, altoDeLaFila);
            }
            hoja.bajar(altoDeLaFila + 20 + 10);
        }
    }

    /** Una foto con su título y sus granos marcados, o el aviso de que no está, en su lugar de la fila. */
    private static void foto(HojaDelReporte hoja, FormatoDelReporte formato, FotoDelReporte foto,
            Optional<ImagenDeLaFoto> imagen, float x, float arriba, float ancho, float altoMaximo) {
        String granos = foto.granos().size() == 1 ? formato.texto("reporte.un_grano")
                : formato.texto("reporte.granos_de_la_foto", foto.granos().size());
        hoja.texto(formato.texto("reporte.foto", foto.numero()) + " · " + granos, hoja.media(), 10,
                HojaDelReporte.TEXTO, x, arriba - 11);
        float abajoDelTitulo = arriba - 18;
        if (imagen.isEmpty()) {
            hoja.rectangulo(x, abajoDelTitulo - 60, ancho, 60, HojaDelReporte.FONDO, HojaDelReporte.BORDE, 6);
            hoja.texto(formato.texto("reporte.foto_no_disponible"), hoja.normal(), 10, HojaDelReporte.SECUNDARIO,
                    x + 12, abajoDelTitulo - 34);
            return;
        }
        ImagenDeLaFoto lista = imagen.get();
        float escala = Math.min(ancho / lista.anchoOriginal(), altoMaximo / lista.altoOriginal());
        float anchoDibujado = lista.anchoOriginal() * escala;
        float altoDibujado = lista.altoOriginal() * escala;
        float abajo = abajoDelTitulo - altoDibujado;
        hoja.imagen(lista.imagen(), x, abajo, anchoDibujado, altoDibujado);
        hoja.marco(x, abajo, anchoDibujado, altoDibujado, HojaDelReporte.BORDE, 0.75f);
        for (GranoMarcado grano : foto.granos()) {
            float gx = x + grano.x() * escala;
            float ancho1 = Math.max(grano.ancho() * escala, 2);
            float alto1 = Math.max(grano.alto() * escala, 2);
            float gy = abajo + altoDibujado - grano.y() * escala - alto1;
            Color color = grano.clase() == ClaseDeGrano.SANO ? HojaDelReporte.EXITO : HojaDelReporte.ERROR;
            hoja.marco(gx, gy, ancho1, alto1, color, 1);
            float lado = Math.clamp(Math.min(ancho1, alto1) * 0.45f, 3.5f, 7f);
            simbolo(hoja, grano.clase(), gx, gy + alto1 - lado, lado);
            if (grano.dudoso()) {
                dudoso(hoja, gx + ancho1 - lado, gy + alto1 - lado, lado);
            }
        }
    }

    /**
     * El símbolo de una clase, en un cuadrado de su color: un chulo blanco si es sano y una equis si no, así no se
     * distinguen solo por el color (RF-11 C2, RP-08).
     *
     * @param x la izquierda del cuadrado
     * @param abajo su borde de abajo
     * @param lado su lado
     */
    private static void simbolo(HojaDelReporte hoja, ClaseDeGrano clase, float x, float abajo, float lado) {
        boolean sano = clase == ClaseDeGrano.SANO;
        hoja.rectangulo(x, abajo, lado, lado, sano ? HojaDelReporte.EXITO : HojaDelReporte.ERROR, null, 0);
        float grosor = Math.max(lado * 0.16f, 0.6f);
        if (sano) {
            hoja.linea(Color.WHITE, grosor, x + lado * 0.2f, abajo + lado * 0.5f, x + lado * 0.42f,
                    abajo + lado * 0.25f, x + lado * 0.8f, abajo + lado * 0.75f);
        } else {
            hoja.linea(Color.WHITE, grosor, x + lado * 0.25f, abajo + lado * 0.25f, x + lado * 0.75f,
                    abajo + lado * 0.75f);
            hoja.linea(Color.WHITE, grosor, x + lado * 0.25f, abajo + lado * 0.75f, x + lado * 0.75f,
                    abajo + lado * 0.25f);
        }
    }

    /** La marca de un grano dudoso: un círculo ámbar con «?», además del color y el símbolo de su clase (D-42). */
    private static void dudoso(HojaDelReporte hoja, float x, float abajo, float lado) {
        hoja.circulo(x + lado / 2, abajo + lado / 2, lado / 2, HojaDelReporte.ADVERTENCIA);
        PDFont fuente = hoja.negrita();
        float tamano = lado * 0.9f;
        float anchoDelSigno = hoja.anchoDe("?", fuente, tamano);
        hoja.texto("?", fuente, tamano, Color.WHITE, x + (lado - anchoDelSigno) / 2, abajo + lado * 0.2f);
    }

    /** La leyenda de las marcas de las fotos. */
    private static void leyenda(HojaDelReporte hoja, FormatoDelReporte formato) {
        hoja.asegurar(18);
        float y = hoja.y() - 12;
        float x = HojaDelReporte.MARGEN;
        hoja.texto(formato.texto("reporte.leyenda"), hoja.media(), 9, HojaDelReporte.SECUNDARIO, x, y);
        x += hoja.anchoDe(formato.texto("reporte.leyenda"), hoja.media(), 9) + 10;
        for (ClaseDeGrano clase : ClaseDeGrano.values()) {
            simbolo(hoja, clase, x, y - 1, 9);
            String nombre = formato.texto("reporte.clase." + clase.name());
            hoja.texto(nombre, hoja.normal(), 9, HojaDelReporte.TEXTO, x + 13, y);
            x += 13 + hoja.anchoDe(nombre, hoja.normal(), 9) + 14;
        }
        dudoso(hoja, x, y - 1, 9);
        hoja.texto(formato.texto("reporte.dudoso"), hoja.normal(), 9, HojaDelReporte.TEXTO, x + 13, y);
        hoja.bajar(20);
    }
}
