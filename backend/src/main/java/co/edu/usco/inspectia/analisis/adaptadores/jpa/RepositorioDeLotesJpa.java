/*
 * Nombre: RepositorioDeLotesJpa.java
 * Descripción: Implementa con JPA el puerto de los lotes guardados (RF-04, RF-10, RF-12, RF-14, D-21).
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.aplicacion.OrigenUsado;
import co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeLotes;
import co.edu.usco.inspectia.analisis.aplicacion.ResultadoGuardado;
import co.edu.usco.inspectia.analisis.aplicacion.VistaDeLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.analisis.dominio.Lote;
import co.edu.usco.inspectia.analisis.dominio.ResultadoDelLote;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** El puerto {@link RepositorioDeLotes} con JPA, sobre la tabla {@code lote}. */
@Component
public class RepositorioDeLotesJpa implements RepositorioDeLotes {

    private final LotesJpa lotes;

    /**
     * Crea el adaptador; Spring le pasa el repositorio de Spring Data.
     *
     * @param lotes la tabla {@code lote}
     */
    public RepositorioDeLotesJpa(LotesJpa lotes) {
        this.lotes = lotes;
    }

    @Override
    public boolean existeCodigo(UUID empresaId, String claveDeCodigo) {
        return lotes.existeCodigo(empresaId, claveDeCodigo);
    }

    /** Guarda el lote y lo vuelve a leer, para responder su fecha de registro, que pone la base. */
    @Override
    public VistaDeLote guardar(Lote lote) {
        LoteEntidad entidad = new LoteEntidad();
        entidad.setEmpresaId(lote.getEmpresaId());
        entidad.setUsuarioId(lote.getUsuarioId());
        entidad.setCodigo(lote.getCodigo());
        entidad.setFechaRecepcion(lote.getFechaRecepcion());
        entidad.setOrigen(lote.getOrigen());
        entidad.setObservaciones(lote.getObservaciones());
        entidad.setEstado(lote.getEstado());
        LoteEntidad guardada = lotes.saveAndFlush(entidad);
        return lotes.vista(lote.getEmpresaId(), guardada.getId()).orElseThrow();
    }

    @Override
    public Optional<VistaDeLote> buscar(UUID empresaId, UUID id) {
        return lotes.vista(empresaId, id);
    }

    @Override
    public Page<VistaDeLote> historial(UUID empresaId, Pageable pagina) {
        return lotes.historial(empresaId, pagina);
    }

    @Override
    public List<OrigenUsado> origenesUsados(UUID empresaId) {
        return lotes.origenesUsados(empresaId);
    }

    @Override
    public Optional<EstadoDeLote> estado(UUID empresaId, UUID id) {
        return lotes.estado(empresaId, id);
    }

    @Override
    public Optional<EstadoDeLote> estadoParaActualizar(UUID empresaId, UUID id) {
        return lotes.paraActualizar(empresaId, id).map(LoteEntidad::getEstado);
    }

    /**
     * Guarda el resultado en la fila ya bloqueada del lote y lo manda a la base en seguida: si la base lo rechaza, el
     * error sale aquí y la transacción se deshace entera.
     */
    @Override
    public void guardarResultado(UUID empresaId, UUID id, ResultadoDelLote resultado, Instant fechaResultado) {
        LoteEntidad lote = lotes.paraActualizar(empresaId, id)
                .orElseThrow(() -> new IllegalStateException("El lote " + id + " no existe"));
        lote.setEstado(EstadoDeLote.ANALIZADO);
        lote.setPorcentajeSanos(resultado.porcentajeSanos());
        lote.setMargen(resultado.margen());
        lote.setCategoria(resultado.categoria());
        lote.setRecomendacion(resultado.recomendacion());
        lote.setSugerirOtraMuestra(resultado.sugerirOtraMuestra());
        lote.setAvisoConfianzaBaja(resultado.avisoConfianzaBaja());
        lote.setFechaResultado(OffsetDateTime.ofInstant(fechaResultado, ZoneOffset.UTC));
        lotes.saveAndFlush(lote);
    }

    @Override
    public Optional<ResultadoGuardado> resultado(UUID empresaId, UUID id) {
        return lotes.resultado(empresaId, id);
    }
}
