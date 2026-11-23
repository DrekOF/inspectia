/*
 * Nombre: ArchivosControlador.java
 * Descripción: Las rutas firmadas de la carpeta de las fotos (D-41): subir y ver un archivo sin sesión, con un enlace
 *              que firma el mismo servidor. La subida también revisa el tipo y el tamaño de la foto.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.carpeta;

import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.ReglasDeFotos;
import co.edu.usco.inspectia.comun.errores.ConflictoExcepcion;
import co.edu.usco.inspectia.comun.errores.EnlaceNoValidoExcepcion;
import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import co.edu.usco.inspectia.comun.errores.PeticionRechazadaExcepcion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las rutas firmadas de la carpeta de las fotos, en {@value AlmacenEnCarpeta#RUTA} (D-41). Hacen las veces de S3: no
 * piden sesión, porque un {@code <img>} no manda el token, y las autoriza la firma del enlace. Sin firma, con una firma
 * que no coincide o vencida, responden 403. La subida además revisa, como pidió Cristhian, que la foto sea JPG o PNG,
 * de hasta 10 MB, también por sus primeros bytes, y no pisa un archivo ya subido. Solo existen con el almacén en
 * carpeta.
 */
@RestController
@RequestMapping(AlmacenEnCarpeta.RUTA)
@ConditionalOnProperty(prefix = "inspectia.archivos", name = "almacen", havingValue = "carpeta",
        matchIfMissing = true)
@Tag(name = "Archivos", description = "Los enlaces firmados de la carpeta de las fotos (D-41). No piden sesión.")
public class ArchivosControlador {

    private final AlmacenEnCarpeta almacen;
    private final FirmaDeEnlaces firmas;
    private final Clock reloj;

    /**
     * Crea el controlador; Spring le pasa sus dependencias.
     *
     * @param almacen la carpeta de las fotos
     * @param firmas la firma de los enlaces
     * @param reloj el reloj de la aplicación
     */
    public ArchivosControlador(AlmacenEnCarpeta almacen, FirmaDeEnlaces firmas, Clock reloj) {
        this.almacen = almacen;
        this.firmas = firmas;
        this.reloj = reloj;
    }

    /**
     * Sube un archivo con un enlace de subida. Lee como mucho los bytes firmados.
     *
     * @param empresa la primera parte de la llave
     * @param lote la segunda parte de la llave
     * @param archivo el nombre del archivo
     * @param vence el vencimiento del enlace
     * @param firma la firma del enlace
     * @param peticion la petición, con los bytes, su tipo y su tamaño
     * @return 200 sin cuerpo
     * @throws IOException si no se puede leer la petición
     */
    @PutMapping("/{empresa}/{lote}/{archivo}")
    @SecurityRequirements
    @Operation(summary = "Subir una foto con un enlace firmado", description = "Sin sesión: la autoriza la firma del "
            + "enlace, que ata el tipo y el tamaño y vence en 10 minutos (403 si no sirve). La foto tiene que ser JPG o "
            + "PNG, de hasta 10 MB, también por dentro (400), y un archivo ya subido no se pisa (409) (D-41).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Los bytes de la foto.",
                    content = {@Content(mediaType = "image/jpeg", schema = @Schema(type = "string", format = "binary")),
                        @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary"))}))
    public ResponseEntity<Void> subir(@PathVariable String empresa, @PathVariable String lote,
            @PathVariable String archivo,
            @Parameter(description = "Vencimiento del enlace.") @RequestParam(name = "vence", required = false)
            String vence,
            @Parameter(description = "Firma del enlace.") @RequestParam(name = "firma", required = false) String firma,
            @Parameter(hidden = true) HttpServletRequest peticion) throws IOException {
        String tipo = peticion.getContentType();
        long bytes = peticion.getContentLengthLong();
        String llave = AlmacenEnCarpeta.llave(empresa, lote, archivo)
                .filter(valida -> firmas.esSubidaValida(valida, segundos(vence), tipo, bytes, firma))
                .orElseThrow(EnlaceNoValidoExcepcion::new);
        FormatoDeFoto formato = FormatoDeFoto.deTipo(tipo)
                .filter(declarado -> archivo.endsWith("." + declarado.getExtension()))
                .orElseThrow(() -> new PeticionRechazadaExcepcion("foto.formato.no_permitido"));
        if (bytes > ReglasDeFotos.MAXIMO_DE_BYTES) {
            throw new PeticionRechazadaExcepcion("foto.tamano_bytes.demasiado_grande");
        }
        if (almacen.existe(llave)) {
            throw new ConflictoExcepcion("foto.ya_subida");
        }
        byte[] contenido = peticion.getInputStream().readNBytes(Math.toIntExact(bytes) + 1);
        if (bytes < 1 || contenido.length != bytes) {
            throw new PeticionRechazadaExcepcion("foto.no_llego");
        }
        if (!formato.empiezaComo(contenido)) {
            throw new PeticionRechazadaExcepcion("foto.formato.no_permitido");
        }
        almacen.guardar(llave, contenido);
        return ResponseEntity.ok().build();
    }

    /**
     * Muestra un archivo con un enlace de vista, como imagen y con la caché privada hasta que vence el enlace.
     *
     * @param empresa la primera parte de la llave
     * @param lote la segunda parte de la llave
     * @param archivo el nombre del archivo
     * @param vence el vencimiento del enlace
     * @param firma la firma del enlace
     * @return la imagen
     */
    @GetMapping("/{empresa}/{lote}/{archivo}")
    @SecurityRequirements
    @Operation(summary = "Ver una foto con un enlace firmado", description = "Sin sesión: la autoriza la firma del "
            + "enlace, que vence en 15 minutos (403 si no sirve). Responde la imagen con el tipo de su formato, nunca "
            + "el que llegó al subirla (D-41). Si la foto ya no está, como una reemplazada, responde 404.")
    public ResponseEntity<Resource> ver(@PathVariable String empresa, @PathVariable String lote,
            @PathVariable String archivo,
            @Parameter(description = "Vencimiento del enlace.") @RequestParam(name = "vence", required = false)
            String vence,
            @Parameter(description = "Firma del enlace.") @RequestParam(name = "firma", required = false) String firma) {
        long venceEn = segundos(vence);
        String llave = AlmacenEnCarpeta.llave(empresa, lote, archivo)
                .filter(valida -> firmas.esVistaValida(valida, venceEn, firma))
                .orElseThrow(EnlaceNoValidoExcepcion::new);
        Path ruta = almacen.ruta(llave);
        if (!Files.isRegularFile(ruta)) {
            throw new NoEncontradoExcepcion("archivo " + llave);
        }
        FormatoDeFoto formato = archivo.endsWith("." + FormatoDeFoto.PNG.getExtension())
                ? FormatoDeFoto.PNG : FormatoDeFoto.JPG;
        long restantes = Math.max(0, venceEn - reloj.instant().getEpochSecond());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(formato.getTipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=" + restantes)
                .body(new FileSystemResource(ruta));
    }

    /** El vencimiento del enlace; uno que falta o no es un número no sirve, y la firma lo rechaza. */
    private static long segundos(String vence) {
        try {
            return vence == null ? Long.MIN_VALUE : Long.parseLong(vence);
        } catch (NumberFormatException excepcion) {
            return Long.MIN_VALUE;
        }
    }
}
