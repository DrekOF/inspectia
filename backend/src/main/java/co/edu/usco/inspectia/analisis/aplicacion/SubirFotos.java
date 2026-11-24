/*
 * Nombre: SubirFotos.java
 * Descripción: Caso de uso de subir las fotos de la muestra de un lote (RF-05): pedir el enlace de subida de una
 *              foto, después de revisar su tipo y su tamaño, y confirmarla cuando el archivo llegó, con la primera
 *              foto que crea la muestra y el reemplazo que borra la anterior; también las de la muestra 2 de un lote
 *              analizado que sugiere otra muestra, sin tocar la 1 (RF-12 C8, RF-14 C6, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
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
 * confirma (RF-05 C5). Solo se reemplazan fotos de la muestra que recibe las fotos, que siempre está pendiente.
 *
 * <p>Qué lote recibe fotos y en qué muestra:
 * <ul>
 *   <li>Un lote pendiente, sin muestra o con su muestra pendiente: la 1.</li>
 *   <li>Un lote analizado que sugiere otra muestra (D-24) y cuya muestra en curso es la 1, terminada: la primera foto
 *       confirmada crea la muestra 2, pendiente y con quien la subió, y la 1 no se toca (D-25). Con la 2 pendiente,
 *       la 2.</li>
 *   <li>Una muestra enviada, en espera o en proceso, ya no recibe fotos: 409 {@code foto.lote_no_pendiente}.</li>
 *   <li>Un lote analizado sin sugerencia, o con sus 2 muestras terminadas, no admite otra muestra: 409
 *       {@code analisis.sin_otra_muestra} (RF-12 C8).</li>
 * </ul>
 */
@Service
public class SubirFotos {

    /** Cuánto dura un enlace para subir una foto (D-41). */
    public static final Duration VIGENCIA_DE_SUBIDA = Duration.ofMinutes(10);

    /** Un UUID escrito como lo escribe Java, en minúsculas. */
    private static final String FORMA_DE_UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    /**
     * Las llaves que arma este caso de uso: {@code <empresa>/<lote>/<uuid>.<jpg|png>}. Es la misma forma que acepta el
     * almacén: una llave con otra forma responde 400 en la llave, y no llega al almacén.
     */
    private static final Pattern LLAVE = Pattern.compile(
            "^(" + FORMA_DE_UUID + ")/(" + FORMA_DE_UUID + ")/" + FORMA_DE_UUID + "\\.(jpg|png)$");

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
     * @throws ConflictoExcepcion si la muestra ya se envió, o si el lote ya tiene su resultado y no admite otra muestra
     *     (409, RF-12 C8)
     * @throws ReglaIncumplidaExcepcion si el número no va de 1 a 3 (RF-05 C2), o si el archivo no es JPG ni PNG o pesa
     *     más de 10 MB (400, RF-05 C3)
     */
    @Transactional(readOnly = true)
    public SubidaDeFoto pedirSubida(UUID empresaId, UUID loteId, int numero, String tipo, long bytes) {
        revisarNumero(numero);
        muestraQueRecibe(empresaId, loteId, lotes.estado(empresaId, loteId));
        FormatoDeFoto formato = revisarArchivo(tipo, bytes);
        String llave = empresaId + "/" + loteId + "/" + UUID.randomUUID() + "." + formato.getExtension();
        return new SubidaDeFoto(llave, almacen.enlaceDeSubida(llave, formato.getTipo(), bytes, VIGENCIA_DE_SUBIDA));
    }

    /**
     * Confirma la foto {@code numero} de la muestra que recibe las fotos cuando su archivo ya llegó al almacén. Revisa
     * cuánto pesa y sus primeros bytes, y solo entonces la guarda, cargada. Confirmar dos veces la misma llave responde
     * la misma foto, así un reintento es seguro.
     *
     * @param empresaId la empresa de la sesión
     * @param usuarioId el usuario de la sesión
     * @param loteId el lote
     * @param numero el número de la foto, de 1 a 3
     * @param llave la llave que dio el enlace de subida
     * @return la foto, con su enlace para verla
     * @throws NoEncontradoExcepcion si el lote no existe o es de otra empresa (404, RNF-03 C1)
     * @throws ConflictoExcepcion si la muestra ya se envió, o si el lote ya tiene su resultado y no admite otra muestra
     *     (409, RF-12 C8)
     * @throws ReglaIncumplidaExcepcion si el número no va de 1 a 3, o si la llave no es de este lote, el archivo no
     *     llegó o no es JPG ni PNG (400)
     */
    @Transactional
    public VistaDeFoto confirmar(UUID empresaId, UUID usuarioId, UUID loteId, int numero, String llave) {
        revisarNumero(numero);
        MuestraQueRecibe destino = muestraQueRecibe(empresaId, loteId, lotes.estadoParaActualizar(empresaId, loteId));
        FormatoDeFoto formato = formatoDeLaLlave(llave, empresaId, loteId);
        // Solo se busca y se reemplaza en la muestra que recibe las fotos: nunca en la 1 ya terminada (D-25).
        Optional<MuestraEnCurso> recibe = Optional.ofNullable(destino.existente());
        if (recibe.isPresent()) {
            Optional<FotoGuardada> misma = muestras.foto(empresaId, recibe.get().id(), numero)
                    .filter(foto -> foto.llave().equals(llave));
            if (misma.isPresent()) {
                return vistas.de(misma.get());
            }
        }
        ArchivoGuardado archivo = almacen.describir(llave).orElseThrow(() -> enLaLlave("foto.no_llego"));
        revisarContenido(formato, archivo);
        MuestraEnCurso muestra = recibe.orElseGet(() -> muestras.crear(empresaId, loteId, destino.numero(),
                usuarioId));
        muestras.foto(empresaId, muestra.id(), numero).ifPresent(anterior -> {
            muestras.borrarFoto(empresaId, anterior.id());
            eventos.publishEvent(new ArchivoReemplazado(anterior.llave()));
        });
        return vistas.de(muestras.guardarFoto(empresaId, muestra.id(), numero, llave, formato, archivo.bytes()));
    }

    /**
     * El número de la foto va de 1 a 3 (RF-05 C2). El controlador también lo revisa, con el mismo límite del dominio; el
     * caso de uso no depende de eso.
     */
    private static void revisarNumero(int numero) {
        if (!ReglasDeFotos.esNumeroValido(numero)) {
            throw new ReglaIncumplidaExcepcion(List.of(new CampoInvalido("numero", "error.valor_invalido")));
        }
    }

    /**
     * La muestra que recibe las fotos del lote: la que ya existe y está pendiente, o la que va a crear la primera foto
     * confirmada.
     *
     * @param existente la muestra pendiente que recibe las fotos, o {@code null} si la crea la primera foto
     * @param numero el número de esa muestra: 1, o 2 para la segunda (D-25)
     */
    private record MuestraQueRecibe(MuestraEnCurso existente, int numero) {
    }

    /**
     * A qué muestra van las fotos del lote (ver la descripción de la clase). Con el lote analizado, la muestra en curso
     * es la de número más alto: la 1 terminada mientras no existe la 2.
     *
     * @throws NoEncontradoExcepcion si el lote no existe o es de otra empresa
     * @throws ConflictoExcepcion si la muestra ya se envió, o si el lote no admite otra muestra
     */
    private MuestraQueRecibe muestraQueRecibe(UUID empresaId, UUID loteId, Optional<EstadoDeLote> estado) {
        EstadoDeLote estadoDelLote = estado.orElseThrow(() -> new NoEncontradoExcepcion("lote " + loteId));
        Optional<MuestraEnCurso> enCurso = muestras.muestraEnCurso(empresaId, loteId);
        if (estadoDelLote == EstadoDeLote.PENDIENTE) {
            if (enCurso.isPresent() && enCurso.get().estado() != EstadoDeMuestra.PENDIENTE) {
                throw new ConflictoExcepcion("foto.lote_no_pendiente");
            }
            return new MuestraQueRecibe(enCurso.orElse(null), 1);
        }
        if (!lotes.sugiereOtraMuestra(empresaId, loteId) || enCurso.isEmpty()) {
            throw new ConflictoExcepcion("analisis.sin_otra_muestra");
        }
        MuestraEnCurso muestra = enCurso.get();
        if (muestra.estado() == EstadoDeMuestra.EN_ESPERA || muestra.estado() == EstadoDeMuestra.EN_PROCESO) {
            throw new ConflictoExcepcion("foto.lote_no_pendiente");
        }
        if (muestra.numero() == 1 && muestra.estado() == EstadoDeMuestra.TERMINADA) {
            return new MuestraQueRecibe(null, 2);
        }
        if (muestra.numero() == 2 && muestra.estado() == EstadoDeMuestra.PENDIENTE) {
            return new MuestraQueRecibe(muestra, 2);
        }
        throw new ConflictoExcepcion("analisis.sin_otra_muestra");
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
