/*
 * Nombre: RegistrarLote.java
 * Descripción: Caso de uso de registrar un lote: aplica las reglas del dominio, revisa que el código no se
 *              repita en la empresa y lo guarda pendiente de análisis (RF-04, RNF-03).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.FechaDeRecepcionFuturaExcepcion;
import co.edu.usco.inspectia.analisis.dominio.Lote;
import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.DatoRepetidoExcepcion;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra un lote (RF-04). La empresa y el usuario salen de la sesión, nunca de la petición (RNF-03).
 *
 * <p>Si la fecha de recepción es posterior a hoy, responde 400 en ese campo (C4); si el código ya existe en
 * la empresa, sin distinguir mayúsculas ni espacios, responde 409 en el código (C2, C3). Si dos peticiones
 * registran el mismo código a la vez, el índice único de la base rechaza la segunda, también con 409.
 */
@Service
public class RegistrarLote {

    /** «Hoy» es el día en Colombia, no en UTC («Fechas» en specs/modelo-datos.md). */
    static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    private final RepositorioDeLotes repositorio;
    private final Clock reloj;

    /**
     * Crea el caso de uso; Spring le pasa sus dependencias.
     *
     * @param repositorio los lotes guardados
     * @param reloj el reloj de la aplicación, en UTC
     */
    public RegistrarLote(RepositorioDeLotes repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /**
     * Registra el lote, pendiente de análisis.
     *
     * @param datos los datos del lote, ya validados
     * @param empresaId la empresa de la sesión
     * @param usuarioId el usuario de la sesión
     * @return el lote registrado
     * @throws ReglaIncumplidaExcepcion si la fecha de recepción es posterior a hoy (400)
     * @throws DatoRepetidoExcepcion si el código ya existe en la empresa (409)
     */
    @Transactional
    public VistaDeLote registrar(DatosDeLote datos, UUID empresaId, UUID usuarioId) {
        LocalDate hoy = LocalDate.ofInstant(reloj.instant(), COLOMBIA);
        Lote lote;
        try {
            lote = Lote.registrar(empresaId, usuarioId, datos.codigo(), datos.fechaRecepcion(), datos.origen(),
                    datos.observaciones(), hoy);
        } catch (FechaDeRecepcionFuturaExcepcion excepcion) {
            throw new ReglaIncumplidaExcepcion(
                    List.of(new CampoInvalido("fechaRecepcion", "lote.fecha_recepcion.no_posterior_a_hoy")));
        }
        if (repositorio.existeCodigo(empresaId, Lote.claveDeCodigo(lote.getCodigo()))) {
            throw new DatoRepetidoExcepcion(List.of(new CampoInvalido("codigo", "lote.empresa_id_codigo.unico")));
        }
        return repositorio.guardar(lote);
    }
}
