/*
 * Nombre: RepositorioDeLotesJpa.java
 * Descripción: Implementa con JPA el puerto de los lotes guardados, con el historial filtrado en una consulta que
 *              comparte su «where» con el conteo, quienes registraron lotes, la sugerencia de otra muestra y el
 *              resultado con el límite de la sugerencia (RF-04, RF-10, RF-12, RF-14, D-21, D-25, D-47).
 * Fecha de creación: 10/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.aplicacion.FiltrosDelHistorial;
import co.edu.usco.inspectia.analisis.aplicacion.OrigenUsado;
import co.edu.usco.inspectia.analisis.aplicacion.Registrador;
import co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeLotes;
import co.edu.usco.inspectia.analisis.aplicacion.ResultadoGuardado;
import co.edu.usco.inspectia.analisis.aplicacion.VistaDeLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.analisis.dominio.Lote;
import co.edu.usco.inspectia.analisis.dominio.ResultadoDelLote;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

/** El puerto {@link RepositorioDeLotes} con JPA, sobre la tabla {@code lote}. */
@Component
public class RepositorioDeLotesJpa implements RepositorioDeLotes {

    /**
     * El carácter de escape del filtro por código: va antes de {@code %}, {@code _} y de sí mismo, así se buscan como
     * caracteres y no como comodines (RF-14 restricción 5).
     */
    static final char ESCAPE = '\\';

    private final LotesJpa lotes;
    private final EntityManager entityManager;

    /**
     * Crea el adaptador; Spring le pasa el repositorio de Spring Data y el {@link EntityManager} compartido de la
     * transacción en curso.
     *
     * @param lotes la tabla {@code lote}
     * @param entityManager para el historial, cuya consulta cambia con los filtros pedidos
     */
    public RepositorioDeLotesJpa(LotesJpa lotes, EntityManager entityManager) {
        this.lotes = lotes;
        this.entityManager = entityManager;
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

    /**
     * Arma la consulta solo con los filtros pedidos: un «where» que comparten la página y su conteo, así el total
     * siempre cuenta los mismos lotes que se listan. Todos los filtros son columnas de {@code lote}, por eso el conteo
     * no necesita el {@code join} con el usuario. El orden es el de siempre ({@link LotesJpa#ORDEN_DEL_HISTORIAL}).
     * Los valores van como parámetros, nunca dentro del texto de la consulta.
     */
    @Override
    public Page<VistaDeLote> historial(UUID empresaId, FiltrosDelHistorial filtros, Pageable pagina) {
        StringBuilder donde = new StringBuilder("where l.empresaId = :empresa");
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("empresa", empresaId);
        Instant inicio = filtros.inicio();
        if (inicio != null) {
            donde.append(" and l.fechaResultado >= :inicio");
            parametros.put("inicio", OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC));
        }
        Instant fin = filtros.fin();
        if (fin != null) {
            donde.append(" and l.fechaResultado < :fin");
            parametros.put("fin", OffsetDateTime.ofInstant(fin, ZoneOffset.UTC));
        }
        if (filtros.estado() != null) {
            donde.append(" and l.estado = :estado");
            parametros.put("estado", filtros.estado());
        }
        if (filtros.categoria() != null) {
            donde.append(" and l.categoria = :categoria");
            parametros.put("categoria", filtros.categoria());
        }
        if (filtros.codigo() != null) {
            donde.append(" and lower(l.codigo) like :codigo escape '").append(ESCAPE).append("'");
            parametros.put("codigo", "%" + sinComodines(filtros.codigo()) + "%");
        }
        if (filtros.usuarioId() != null) {
            donde.append(" and l.usuarioId = :usuario");
            parametros.put("usuario", filtros.usuarioId());
        }
        TypedQuery<VistaDeLote> consulta = entityManager.createQuery(
                LotesJpa.VISTA + donde + LotesJpa.ORDEN_DEL_HISTORIAL, VistaDeLote.class);
        TypedQuery<Long> conteo = entityManager.createQuery("select count(l) from LoteEntidad l " + donde, Long.class);
        parametros.forEach((nombre, valor) -> {
            consulta.setParameter(nombre, valor);
            conteo.setParameter(nombre, valor);
        });
        consulta.setFirstResult(Math.toIntExact(pagina.getOffset()));
        consulta.setMaxResults(pagina.getPageSize());
        return PageableExecutionUtils.getPage(consulta.getResultList(), pagina, conteo::getSingleResult);
    }

    /**
     * El texto buscado con {@link #ESCAPE} antes de {@code %}, {@code _} y del propio escape, para el {@code like}.
     *
     * @param texto lo que se busca dentro del código
     * @return el texto, sin comodines
     */
    static String sinComodines(String texto) {
        StringBuilder escapado = new StringBuilder(texto.length());
        for (char caracter : texto.toCharArray()) {
            if (caracter == '%' || caracter == '_' || caracter == ESCAPE) {
                escapado.append(ESCAPE);
            }
            escapado.append(caracter);
        }
        return escapado.toString();
    }

    @Override
    public List<Registrador> registradores(UUID empresaId) {
        return lotes.registradores(empresaId);
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

    @Override
    public boolean sugiereOtraMuestra(UUID empresaId, UUID id) {
        return lotes.sugiereOtraMuestra(empresaId, id).orElse(false);
    }

    /**
     * Guarda el resultado en la fila ya bloqueada del lote y lo manda a la base en seguida: si la base lo rechaza, el
     * error sale aquí y la transacción se deshace entera. Escribe también el límite de la sugerencia y su categoría, o
     * los deja vacíos, como pide el CHECK {@code lote_sugerencia_con_su_limite} (D-47), y deja vacía la ruta del
     * reporte: con la muestra 2 el resultado cambia y el PDF anterior ya no sirve (D-25, RF-15). El archivo lo borra el
     * módulo del reporte.
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
        lote.setLimiteSugerencia(resultado.limiteSugerencia());
        lote.setCategoriaDelLimite(resultado.categoriaDelLimite());
        lote.setAvisoConfianzaBaja(resultado.avisoConfianzaBaja());
        lote.setFechaResultado(OffsetDateTime.ofInstant(fechaResultado, ZoneOffset.UTC));
        lote.setReporteRuta(null);
        lotes.saveAndFlush(lote);
    }

    @Override
    public Optional<ResultadoGuardado> resultado(UUID empresaId, UUID id) {
        return lotes.resultado(empresaId, id);
    }
}
