/*
 * Nombre: ResultadoRespuesta.java
 * Descripción: El resultado de un lote y el detalle de sus muestras como lo responde la API: las cifras guardadas, con
 *              el límite de la sugerencia y su categoría, las fotos con su enlace y los granos marcados, sin la
 *              confianza (RF-10, RF-11, RF-12, RF-14 C3, D-25, D-47).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.DetalleDeMuestra;
import co.edu.usco.inspectia.analisis.aplicacion.DetalleDelResultado;
import co.edu.usco.inspectia.analisis.aplicacion.FotoMarcada;
import co.edu.usco.inspectia.analisis.aplicacion.GranoMarcado;
import co.edu.usco.inspectia.analisis.aplicacion.MuestraGuardada;
import co.edu.usco.inspectia.analisis.aplicacion.ResultadoGuardado;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * El resultado de un lote y el detalle de sus muestras (RF-14 C3). Las cifras son las guardadas: no se recalculan con
 * los rangos de hoy (RF-13 C3). Mientras el lote está pendiente, las del resultado van vacías y cada foto, sin granos.
 * Mientras la muestra 2 espera o se analiza, el lote trae el resultado de la 1 y la 2 va sin conteos (D-25).
 *
 * @param lote el lote y su resultado
 * @param muestras sus muestras, por número
 */
@Schema(description = "El resultado de un lote y el detalle de sus muestras, con las fotos marcadas (RF-10, RF-11, "
        + "RF-12, RF-14 C3).")
public record ResultadoRespuesta(
        @Schema(description = "El lote y su resultado guardado.") LoteDelResultado lote,
        @Schema(description = "Las muestras del lote, por número: una, o dos con la segunda (D-25).")
        List<MuestraDelResultado> muestras) {

    /**
     * El lote y su resultado guardado.
     *
     * @param id identificador del lote
     * @param codigo su código
     * @param fechaRecepcion su fecha de recepción
     * @param origen la finca o el proveedor
     * @param estado {@code PENDIENTE} o {@code ANALIZADO}
     * @param fechaResultado la fecha del resultado, o vacía
     * @param porcentajeSanos el porcentaje corregido, o vacío
     * @param margen su margen, o vacío
     * @param categoria la categoría probable, o vacía
     * @param recomendacion el texto de la recomendación, o vacío
     * @param sugerirOtraMuestra si se sugiere otra muestra, o vacío
     * @param limiteSugerencia el límite por el que se sugiere otra muestra, o vacío
     * @param categoriaDelLimite la categoría de ese límite, o vacía
     * @param avisoConfianzaBaja si lleva el aviso de confianza baja, o vacío
     * @param totalGranos el total de granos de las muestras terminadas, o vacío
     * @param granosSanos los que el modelo marcó como sanos, o vacío
     * @param porcentajeContado el porcentaje que contó el modelo, o vacío
     */
    @Schema(description = "El lote y su resultado guardado. Mientras está pendiente, las cifras del resultado van "
            + "vacías.")
    public record LoteDelResultado(
            @Schema(description = "Identificador del lote.") UUID id,
            @Schema(description = "Código del lote.", example = "L-0143") String codigo,
            @Schema(description = "Fecha de recepción.", example = "2026-10-20") LocalDate fechaRecepcion,
            @Schema(description = "Finca o proveedor.", example = "La Esperanza") String origen,
            @Schema(description = "PENDIENTE (de análisis) o ANALIZADO.", allowableValues = {"PENDIENTE", "ANALIZADO"},
                    example = "ANALIZADO") String estado,
            @Schema(description = "Fecha del resultado: cuándo terminó la última muestra; vacía mientras está "
                    + "pendiente.", example = "2026-11-23T15:00:03Z") Instant fechaResultado,
            @Schema(description = "Porcentaje de granos sanos corregido con los recall del modelo (D-39), de 0 a 100 y "
                    + "con 1 decimal; vacío mientras está pendiente.", example = "90.6") BigDecimal porcentajeSanos,
            @Schema(description = "Margen del porcentaje, en puntos y con 1 decimal (P-19). La página muestra el rango "
                    + "probable, el porcentaje más y menos el margen, recortado a 0–100 % (D-43); vacío mientras está "
                    + "pendiente.", example = "7.5") BigDecimal margen,
            @Schema(description = "Categoría probable: la oficial se confirma con la prueba de corte (D-10); vacía "
                    + "mientras está pendiente.", allowableValues = {"PREMIUM", "ESTANDAR", "CORRIENTE",
                        "FUERA_DE_NORMA"}, example = "ESTANDAR") String categoria,
            @Schema(description = "Texto de la recomendación de su rango, copiado tal cual lo escribió la empresa "
                    + "(RF-13); vacío mientras está pendiente.",
                    example = "Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de "
                            + "categoría.") String recomendacion,
            @Schema(description = "Si se sugiere analizar otra muestra: el porcentaje quedó a menos de 1 punto de un "
                    + "límite de la empresa, y el lote tiene una sola muestra terminada (D-24, D-25). Sigue verdadero "
                    + "mientras se toma y se analiza la muestra 2; cuando termina, queda falso. Vacío mientras está "
                    + "pendiente.", example = "true") Boolean sugerirOtraMuestra,
            @Schema(description = "El límite inferior de los rangos de la empresa por el que se sugiere otra muestra: "
                    + "el más cercano al porcentaje y, en empate, el más alto; el 0 de Fuera de norma nunca cuenta. Se "
                    + "guarda con el resultado, así el aviso «El resultado quedó a 0,6 puntos del límite de Estándar "
                    + "(90 %).» no cambia si la empresa cambia sus rangos (RF-13 C3, D-47). Vacío sin sugerencia.",
                    example = "90.0") BigDecimal limiteSugerencia,
            @Schema(description = "La categoría de ese límite, que puede ser otra que la del lote: con 95,4 %, el "
                    + "lote es Estándar y el límite es el de PREMIUM (96). Vacía sin sugerencia (D-47).",
                    allowableValues = {"PREMIUM", "ESTANDAR", "CORRIENTE"}, example = "ESTANDAR")
            String categoriaDelLimite,
            @Schema(description = "Si alguna muestra salió con el aviso de confianza baja (RF-09); vacío mientras "
                    + "está pendiente.", example = "false") Boolean avisoConfianzaBaja,
            @Schema(description = "Total de granos de las muestras terminadas; vacío mientras está pendiente.",
                    example = "100") Integer totalGranos,
            @Schema(description = "Granos que el modelo marcó como sanos en las muestras terminadas; vacío mientras "
                    + "está pendiente.", example = "87") Integer granosSanos,
            @Schema(description = "Porcentaje que contó el modelo, sin corregir, con 1 decimal; vacío mientras está "
                    + "pendiente.", example = "87.0") BigDecimal porcentajeContado) {
    }

    /**
     * Una muestra del lote.
     *
     * @param numero su número
     * @param estado su estado
     * @param analizadaPor quien la envió a analizar
     * @param modelo el modelo que la analizó, o vacío
     * @param totalGranos su total de granos, o vacío
     * @param granosSanos sus granos sanos, o vacío
     * @param porcentajeContado su porcentaje contado, o vacío
     * @param fechaEnvio cuándo se envió, o vacía
     * @param fechaFin cuándo terminó, o vacía
     * @param fotos sus fotos, por número
     * @param motivoDevolucion por qué volvió de la cola sin resultado, o vacío (D-44)
     * @param repeticiones las fotos que RF-09 pidió repetir
     * @param fotosPorRepetir sus fotos por repetir, mientras no termina
     * @param avisoConfianzaBaja su aviso de confianza baja, o vacío mientras no termina (RF-09)
     * @param porcentajeMaxDudosos la copia del máximo de dudosos que usó, o vacía
     */
    @Schema(description = "Una muestra del lote. Mientras no termina, el modelo, los conteos y la fecha de fin van "
            + "vacíos, y sus fotos, sin granos.")
    public record MuestraDelResultado(
            @Schema(description = "Su número: 1, o 2 para la segunda (D-25).", example = "1") int numero,
            @Schema(description = "Su estado.", allowableValues = {"PENDIENTE", "EN_ESPERA", "EN_PROCESO",
                "TERMINADA"}, example = "TERMINADA") String estado,
            @Schema(description = "Quien la envió a analizar.") AnalizadaPor analizadaPor,
            @Schema(description = "El modelo de IA que la analizó (RF-08 C3); vacío mientras no termina.")
            ModeloDeLaMuestra modelo,
            @Schema(description = "Su total de granos; vacío mientras no termina.", example = "100") Integer totalGranos,
            @Schema(description = "Los granos que el modelo marcó como sanos; vacío mientras no termina.",
                    example = "87") Integer granosSanos,
            @Schema(description = "Su porcentaje contado, con 1 decimal; vacío mientras no termina.", example = "87.0")
            BigDecimal porcentajeContado,
            @Schema(description = "Cuándo se envió a analizar; vacía si no se ha enviado.") Instant fechaEnvio,
            @Schema(description = "Cuándo terminó; vacía mientras no termina.") Instant fechaFin,
            @Schema(description = "Sus fotos, por número, con sus granos marcados.") List<FotoDelResultado> fotos,
            @Schema(description = "Por qué volvió de la cola sin resultado (D-44); vacío si no volvió o si la devolvió "
                    + "una foto.", allowableValues = {"POCOS_GRANOS", "SIN_RESPUESTA_DE_LA_IA"}, nullable = true,
                    example = "POCOS_GRANOS") String motivoDevolucion,
            @Schema(description = "Cuántas fotos pidió repetir RF-09 por granos dudosos, de 0 a 2.", example = "0")
            int repeticiones,
            @Schema(description = "Las fotos de la muestra que están POR_REPETIR, por número, con su motivo; vacía si "
                    + "no hay o si ya terminó.") List<AnalisisRespuesta.FotoPorRepetir> fotosPorRepetir,
            @Schema(description = "Si esta muestra terminó con el aviso de confianza baja (RF-09 C4); vacío mientras no "
                    + "termina.", example = "false", nullable = true) Boolean avisoConfianzaBaja,
            @Schema(description = "La copia del máximo de dudosos que usó la muestra (P-15), con 1 decimal; vacía si "
                    + "RF-09 todavía no la evaluó. Con la muestra terminada, siempre llena.", example = "10.0",
                    nullable = true) BigDecimal porcentajeMaxDudosos) {
    }

    /**
     * Quien envió a analizar una muestra.
     *
     * @param id su identificador
     * @param nombre su nombre
     */
    @Schema(description = "Quien envió a analizar la muestra («Analizada por»).")
    public record AnalizadaPor(
            @Schema(description = "Identificador del usuario.") UUID id,
            @Schema(description = "Nombre del usuario.", example = "Carlos Ramírez") String nombre) {
    }

    /**
     * El modelo que analizó una muestra.
     *
     * @param nombre su nombre
     * @param version su versión
     */
    @Schema(description = "El modelo de IA que analizó la muestra (RF-08 C3).")
    public record ModeloDeLaMuestra(
            @Schema(description = "Nombre del modelo.", example = "clasificador-falso") String nombre,
            @Schema(description = "Versión del modelo.", example = "1.0") String version) {
    }

    /**
     * Una foto de la muestra con sus granos marcados.
     *
     * @param numero su número
     * @param vista el enlace para verla
     * @param granos sus granos
     */
    @Schema(description = "Una foto de la muestra, con el enlace para verla y sus granos marcados (RF-11).")
    public record FotoDelResultado(
            @Schema(description = "Su número en la muestra, de 1 a 3.", example = "1") int numero,
            @Schema(description = "El enlace para verla, que vence en 15 minutos (D-41).") FotoRespuesta.Enlace vista,
            @Schema(description = "Sus granos, en el orden en que se guardaron; vacía mientras la muestra no termina.")
            List<GranoDelResultado> granos) {
    }

    /**
     * Un grano marcado, sin su confianza.
     *
     * @param x posición horizontal
     * @param y posición vertical
     * @param ancho ancho
     * @param alto alto
     * @param clase {@code SANO} o {@code NO_SANO}
     * @param dudoso si su confianza es menor que el umbral de su muestra
     */
    @Schema(description = "Un grano marcado en la foto: un rectángulo en píxeles de la foto original, su clase y si es "
            + "dudoso. La confianza no se responde (RF-11 C4, D-42).")
    public record GranoDelResultado(
            @Schema(description = "Posición horizontal de su esquina superior izquierda, en píxeles.", example = "4")
            int x,
            @Schema(description = "Posición vertical de su esquina superior izquierda, en píxeles.", example = "6")
            int y,
            @Schema(description = "Ancho, en píxeles.", example = "5") int ancho,
            @Schema(description = "Alto, en píxeles.", example = "8") int alto,
            @Schema(description = "Su clase: verde con chulo si es SANO y rojo con equis si es NO_SANO (RF-11).",
                    allowableValues = {"SANO", "NO_SANO"}, example = "SANO") String clase,
            @Schema(description = "Si es dudoso: su confianza es menor que el umbral que copió su muestra (P-15). Lleva "
                    + "una marca «?» además del color y el símbolo de su clase (D-42).", example = "false")
            boolean dudoso) {
    }

    /**
     * La respuesta del resultado de un lote.
     *
     * @param detalle el resultado y sus muestras
     * @return la respuesta
     */
    static ResultadoRespuesta de(DetalleDelResultado detalle) {
        ResultadoGuardado lote = detalle.lote();
        LoteDelResultado delLote = new LoteDelResultado(lote.id(), lote.codigo(), lote.fechaRecepcion(), lote.origen(),
                lote.estado().name(), lote.fechaResultado() == null ? null : lote.fechaResultado().toInstant(),
                lote.porcentajeSanos(), lote.margen(), lote.categoria(), lote.recomendacion(),
                lote.sugerirOtraMuestra(), lote.limiteSugerencia(), lote.categoriaDelLimite(),
                lote.avisoConfianzaBaja(), detalle.totalGranos(), detalle.granosSanos(), detalle.porcentajeContado());
        return new ResultadoRespuesta(delLote, detalle.muestras().stream().map(ResultadoRespuesta::muestra).toList());
    }

    private static MuestraDelResultado muestra(DetalleDeMuestra detalle) {
        MuestraGuardada muestra = detalle.muestra();
        ModeloDeLaMuestra modelo = muestra.modeloNombre() == null ? null
                : new ModeloDeLaMuestra(muestra.modeloNombre(), muestra.modeloVersion());
        return new MuestraDelResultado(muestra.numero(), muestra.estado().name(),
                new AnalizadaPor(muestra.usuarioId(), muestra.usuarioNombre()), modelo, muestra.totalGranos(),
                muestra.granosSanos(), detalle.porcentajeContado(), muestra.fechaEnvio(), muestra.fechaFin(),
                detalle.fotos().stream().map(ResultadoRespuesta::foto).toList(),
                muestra.motivoDevolucion() == null ? null : muestra.motivoDevolucion().name(), muestra.repeticiones(),
                AnalisisRespuesta.FotoPorRepetir.de(detalle.fotosPorRepetir()), muestra.avisoConfianzaBaja(),
                muestra.porcentajeMaxDudosos());
    }

    private static FotoDelResultado foto(FotoMarcada foto) {
        return new FotoDelResultado(foto.numero(), new FotoRespuesta.Enlace(foto.vista().url(), foto.vista().vence()),
                foto.granos().stream().map(ResultadoRespuesta::grano).toList());
    }

    private static GranoDelResultado grano(GranoMarcado grano) {
        return new GranoDelResultado(grano.x(), grano.y(), grano.ancho(), grano.alto(), grano.clase().name(),
                grano.dudoso());
    }
}
