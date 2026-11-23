/*
 * Nombre: Lote.java
 * Descripción: Un lote que se registra: sus datos, su estado inicial y las reglas del registro (RF-04).
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Un lote que se registra (RF-04). Solo se crea con {@link #registrar}, que aplica las reglas: la fecha de
 * recepción no puede ser posterior a hoy (C4) ni de hace más de un año (restricción 4), y el lote empieza pendiente
 * de análisis. Que el código no se repita en la empresa lo revisa el caso de uso con el repositorio, comparando
 * {@link #claveDeCodigo}.
 *
 * <p>Los textos llegan ya recortados, sin espacios al inicio ni al final.
 */
public final class Lote {

    private final UUID empresaId;
    private final UUID usuarioId;
    private final String codigo;
    private final LocalDate fechaRecepcion;
    private final String origen;
    private final String observaciones;
    private final EstadoDeLote estado;

    private Lote(UUID empresaId, UUID usuarioId, String codigo, LocalDate fechaRecepcion, String origen,
            String observaciones, EstadoDeLote estado) {
        this.empresaId = empresaId;
        this.usuarioId = usuarioId;
        this.codigo = codigo;
        this.fechaRecepcion = fechaRecepcion;
        this.origen = origen;
        this.observaciones = observaciones;
        this.estado = estado;
    }

    /**
     * Registra un lote nuevo, pendiente de análisis.
     *
     * @param empresaId la empresa de la sesión (RNF-03)
     * @param usuarioId el usuario que lo registra
     * @param codigo el código del lote
     * @param fechaRecepcion la fecha en que la empresa recibió el lote
     * @param origen la finca o el proveedor
     * @param observaciones las observaciones; {@code null} si no tiene
     * @param hoy el día de hoy en Colombia
     * @return el lote
     * @throws FechaDeRecepcionFuturaExcepcion si la fecha de recepción es posterior a hoy (RF-04 C4)
     * @throws FechaDeRecepcionAntiguaExcepcion si la fecha de recepción es de hace más de un año (RF-04,
     *         restricción 4)
     */
    public static Lote registrar(UUID empresaId, UUID usuarioId, String codigo, LocalDate fechaRecepcion,
            String origen, String observaciones, LocalDate hoy) {
        Objects.requireNonNull(empresaId, "empresaId");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(fechaRecepcion, "fechaRecepcion");
        Objects.requireNonNull(origen, "origen");
        if (fechaRecepcion.isAfter(hoy)) {
            throw new FechaDeRecepcionFuturaExcepcion(fechaRecepcion, hoy);
        }
        LocalDate primeraFecha = primeraFechaDeRecepcion(hoy);
        if (fechaRecepcion.isBefore(primeraFecha)) {
            throw new FechaDeRecepcionAntiguaExcepcion(fechaRecepcion, primeraFecha);
        }
        return new Lote(empresaId, usuarioId, codigo, fechaRecepcion, origen, observaciones, EstadoDeLote.PENDIENTE);
    }

    /**
     * La fecha de recepción más antigua que se acepta: hace exactamente un año, el mismo día del año anterior. Si
     * hoy es 29 de febrero, es el 28 de febrero del año anterior (RF-04, restricción 4).
     *
     * @param hoy el día de hoy en Colombia
     * @return la fecha de hace un año
     */
    public static LocalDate primeraFechaDeRecepcion(LocalDate hoy) {
        return hoy.minusYears(1);
    }

    /**
     * La clave con que se compara el código de un lote: sin espacios al inicio ni al final y en minúsculas.
     * Así «l-001 » y «L-001» son el mismo código (RF-04 C3), como en el índice único de la base, sobre
     * {@code lower(btrim(codigo))}.
     *
     * @param codigo el código
     * @return su clave
     */
    public static String claveDeCodigo(String codigo) {
        return codigo.strip().toLowerCase(Locale.ROOT);
    }

    /** @return la empresa del lote */
    public UUID getEmpresaId() {
        return empresaId;
    }

    /** @return el usuario que lo registró */
    public UUID getUsuarioId() {
        return usuarioId;
    }

    /** @return el código del lote */
    public String getCodigo() {
        return codigo;
    }

    /** @return la fecha de recepción */
    public LocalDate getFechaRecepcion() {
        return fechaRecepcion;
    }

    /** @return la finca o el proveedor */
    public String getOrigen() {
        return origen;
    }

    /** @return las observaciones, o {@code null} */
    public String getObservaciones() {
        return observaciones;
    }

    /** @return el estado */
    public EstadoDeLote getEstado() {
        return estado;
    }
}
