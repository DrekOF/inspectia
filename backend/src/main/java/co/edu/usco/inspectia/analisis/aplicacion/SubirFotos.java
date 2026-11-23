/*
 * Nombre: SubirFotos.java
 * Descripción: Caso de uso de subir las fotos de la muestra de un lote (RF-05): pedir el enlace de subida de una
 *              foto, después de revisar su tipo y su tamaño, y confirmarla cuando el archivo llegó, con la primera
 *              foto que crea la muestra y el reemplazo que borra la anterior.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFotoNoPermitidoExcepcion;
import co.edu.usco.inspectia.analisis.dominio.FotoDemasiadoGrandeExcepcion;
import co.edu.usco.inspectia.analisis.dominio.ReglasDeFotos;
import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.ConflictoExcepcion;
import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sube las fotos de la muestra en curso de un lote (RF-05). La página sigue el mismo flujo que con S3 (D-41): pide el
 * enlace, sube el archivo directo al almacén y confirma la foto. La empresa sale siempre de la sesión (RNF-03), y
 * cualquier usuario final de la empresa puede seguir un lote pendiente (RF-14 C5).
 *
 * <p>La primera foto confirmada crea la muestra 1, pendiente y con quien la subió. Una foto nueva con el número de
 * otra la reemplaza: la anterior se borra de la base en la misma transacción y del almacén cuando la transacción se
 * confirma (RF-05 C5).
 */
@Service
public class SubirFotos {

    /** Cuánto dura un enlace para subir una foto (D-41). */
    public static final Duration VIGENCIA_DE_SUBIDA = Duration.ofMinutes(10);

    /** Las llaves que arma este caso de uso: {@code <empresa>/<lote>/<uuid>.<jpg|png>}. */
    private static final Pattern LLAVE = Pattern.compile(
            "^([0-9a-f-]{36})/([0-9a-f-]{36})/[0-9a-f-]{36}\\.(jpg|png)$");

    private final RepositorioDeLotes lotes;
    private final RepositorioDeMuestras muestras;
    private final AlmacenDeArchivos almacen;
    private final VistasDeFotos vistas;
    private final ApplicationEventPublisher eventos;

    /**
     * Crea el caso de uso; Spring le pasa sus dependencias.
     *
     * @param lotes los lotes guardados
     * @param muestras las muestras y sus fotos
     * @param almacen el almacén de los archivos
     * @param vistas las vistas de las fotos, con su enlace para verlas
     * @param eventos para borrar el archivo de una foto reemplazada después de confirmar
     */
    public SubirFotos(RepositorioDeLotes lotes, RepositorioDeMuestras muestras, AlmacenDeArchivos almacen,
            VistasDeFotos vistas, ApplicationEventPublisher eventos) {
        this.lotes = lotes;
        this.muestras = muestras;
        this.almacen = almacen;
        this.vistas = vistas;
        this.eventos = eventos;
    }

    /**
     * El enlace para subir la foto {@code numero} de la muestra en curso. Antes revisa el lote y el archivo.
     *
     * @param empresaId la empresa de la sesión
     * @param loteId el lote
     * @param numero el número de la foto, de 1 a 3
     * @param tipo el tipo del archivo, como {@code image/jpeg}
     * @param bytes cuánto pesa
     * @return la llave del archivo y el enlace para subirlo
     * @throws NoEncontradoExcepcion si el lote no existe o es de otra empresa (404, RNF-03 C1)
     * @throws ConflictoExcepcion si el lote o su muestra ya no están pendientes (409)
     * @throws ReglaIncumplidaExcepcion si el archivo no es JPG ni PNG o pesa más de 10 MB (400, RF-05 C3)
     */
    @Transactional(readOnly = true)
    public SubidaDeFoto pedirSubida(UUID empresaId, UUID loteId, int numero, String tipo, long bytes) {
        revisarQueSigaPendiente(empresaId, loteId, lotes.estado(empresaId, loteId));
        FormatoDeFoto formato = revisarArchivo(tipo, bytes);
        String llave = empresaId + "/" + loteId + "/" + UUID.randomUUID() + "." + formato.getExtension();
        return new SubidaDeFoto(llave, almacen.enlaceDeSubida(llave, formato.getTipo(), bytes, VIGENCIA_DE_SUBIDA));
    }

    /**
     * Confirma la foto {@code numero} de la muestra en curso cuando su archivo ya llegó al almacén. Revisa cuánto pesa
     * y sus primeros bytes, y solo entonces la guarda, cargada. Confirmar dos veces la misma llave responde la misma
     * foto, así un reintento es seguro.
     *
     * @param empresaId la empresa de la sesión
     * @param usuarioId el usuario de la sesión
     * @param loteId el lote
     * @param numero el número de la foto, de 1 a 3
     * @param llave la llave que dio el enlace de subida
     * @return la foto, con su enlace para verla
     * @throws NoEncontradoExcepcion si el lote no existe o es de otra empresa (404, RNF-03 C1)
     * @throws ConflictoExcepcion si el lote o su muestra ya no están pendientes (409)
     * @throws ReglaIncumplidaExcepcion si la llave no es de este lote, el archivo no llegó o no es JPG ni PNG (400)
     */
    @Transactional
    public VistaDeFoto confirmar(UUID empresaId, UUID usuarioId, UUID loteId, int numero, String llave) {
        revisarQueSigaPendiente(empresaId, loteId, lotes.estadoParaActualizar(empresaId, loteId));
        FormatoDeFoto formato = formatoDeLaLlave(llave, empresaId, loteId);
        Optional<MuestraEnCurso> enCurso = muestras.muestraEnCurso(empresaId, loteId);
        if (enCurso.isPresent()) {
            Optional<FotoGuardada> misma = muestras.foto(empresaId, enCurso.get().id(), numero)
                    .filter(foto -> foto.llave().equals(llave));
            if (misma.isPresent()) {
                return vistas.de(misma.get());
            }
        }
        ArchivoGuardado archivo = almacen.describir(llave).orElseThrow(() -> enLaLlave("foto.no_llego"));
        revisarContenido(formato, archivo);
        MuestraEnCurso muestra = enCurso.orElseGet(() -> muestras.crear(empresaId, loteId, 1, usuarioId));
        muestras.foto(empresaId, muestra.id(), numero).ifPresent(anterior -> {
            muestras.borrarFoto(empresaId, anterior.id());
            eventos.publishEvent(new ArchivoReemplazado(anterior.llave()));
        });
        return vistas.de(muestras.guardarFoto(empresaId, muestra.id(), numero, llave, formato, archivo.bytes()));
    }

    /** Solo un lote pendiente, con su muestra en curso pendiente o sin muestra, recibe fotos (RF-05). */
    private void revisarQueSigaPendiente(UUID empresaId, UUID loteId, Optional<EstadoDeLote> estado) {
        EstadoDeLote estadoDelLote = estado.orElseThrow(() -> new NoEncontradoExcepcion("lote " + loteId));
        boolean muestraPendiente = muestras.muestraEnCurso(empresaId, loteId)
                .map(muestra -> muestra.estado() == EstadoDeMuestra.PENDIENTE).orElse(true);
        if (estadoDelLote != EstadoDeLote.PENDIENTE || !muestraPendiente) {
            throw new ConflictoExcepcion("foto.lote_no_pendiente");
        }
    }

    private static FormatoDeFoto revisarArchivo(String tipo, long bytes) {
        try {
            return ReglasDeFotos.revisarArchivo(tipo, bytes);
        } catch (FormatoDeFotoNoPermitidoExcepcion excepcion) {
            throw new ReglaIncumplidaExcepcion(List.of(new CampoInvalido("tipo", "foto.formato.no_permitido")));
        } catch (FotoDemasiadoGrandeExcepcion excepcion) {
            throw new ReglaIncumplidaExcepcion(
                    List.of(new CampoInvalido("tamanoBytes", "foto.tamano_bytes.demasiado_grande")));
        }
    }

    /** El archivo subido tiene que pesar hasta 10 MB y empezar como su formato (RF-05 C3). */
    private static void revisarContenido(FormatoDeFoto formato, ArchivoGuardado archivo) {
        if (archivo.bytes() < 1) {
            throw enLaLlave("foto.no_llego");
        }
        try {
            ReglasDeFotos.revisarArchivo(formato.getTipo(), archivo.bytes());
            ReglasDeFotos.revisarContenido(formato, archivo.primeros());
        } catch (FormatoDeFotoNoPermitidoExcepcion excepcion) {
            throw enLaLlave("foto.formato.no_permitido");
        } catch (FotoDemasiadoGrandeExcepcion excepcion) {
            throw enLaLlave("foto.tamano_bytes.demasiado_grande");
        }
    }

    /** El formato de una llave que armó este caso de uso para el mismo lote y la misma empresa (RNF-03). */
    private static FormatoDeFoto formatoDeLaLlave(String llave, UUID empresaId, UUID loteId) {
        Matcher partes = llave == null ? null : LLAVE.matcher(llave);
        if (partes == null || !partes.matches() || !partes.group(1).equals(empresaId.toString())
                || !partes.group(2).equals(loteId.toString())) {
            throw enLaLlave("error.valor_invalido");
        }
        return FormatoDeFoto.deExtension(partes.group(3)).orElseThrow(() -> enLaLlave("error.valor_invalido"));
    }

    private static ReglaIncumplidaExcepcion enLaLlave(String clave) {
        return new ReglaIncumplidaExcepcion(List.of(new CampoInvalido("llave", clave)));
    }
}
