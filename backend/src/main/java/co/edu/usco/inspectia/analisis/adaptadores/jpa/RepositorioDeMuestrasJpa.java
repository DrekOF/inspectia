/*
 * Nombre: RepositorioDeMuestrasJpa.java
 * Descripción: El puerto RepositorioDeMuestras con JPA, sobre las tablas muestra, foto y grano (RF-05, RF-10, RNF-02,
 *              RNF-03).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.aplicacion.AvanceDelAnalisis;
import co.edu.usco.inspectia.analisis.aplicacion.FotoGuardada;
import co.edu.usco.inspectia.analisis.aplicacion.GranoDeFoto;
import co.edu.usco.inspectia.analisis.aplicacion.MuestraEnCurso;
import co.edu.usco.inspectia.analisis.aplicacion.MuestraGuardada;
import co.edu.usco.inspectia.analisis.aplicacion.MuestraTomada;
import co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeMuestras;
import co.edu.usco.inspectia.analisis.aplicacion.TerminoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.ConteoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * El puerto {@link RepositorioDeMuestras} con JPA, sobre las tablas {@code muestra}, {@code foto} y {@code grano}.
 * Terminar y devolver una muestra son {@code UPDATE} con una condición sobre su estado y su hora de inicio: así nunca
 * se guarda el resultado de una muestra que otra ejecución ya tomó.
 */
@Component
public class RepositorioDeMuestrasJpa implements RepositorioDeMuestras {

    private final MuestrasJpa muestras;
    private final FotosJpa fotos;
    private final GranosJpa granos;

    /**
     * Crea el adaptador; Spring le pasa los repositorios de Spring Data.
     *
     * @param muestras la tabla {@code muestra}
     * @param fotos la tabla {@code foto}
     * @param granos la tabla {@code grano}
     */
    public RepositorioDeMuestrasJpa(MuestrasJpa muestras, FotosJpa fotos, GranosJpa granos) {
        this.muestras = muestras;
        this.fotos = fotos;
        this.granos = granos;
    }

    @Override
    public Optional<MuestraEnCurso> muestraEnCurso(UUID empresaId, UUID loteId) {
        return muestras.delLote(empresaId, loteId).stream().findFirst().map(RepositorioDeMuestrasJpa::enCurso);
    }

    @Override
    public MuestraEnCurso crear(UUID empresaId, UUID loteId, int numero, UUID usuarioId) {
        MuestraEntidad muestra = new MuestraEntidad();
        muestra.setEmpresaId(empresaId);
        muestra.setLoteId(loteId);
        muestra.setNumero(numero);
        muestra.setUsuarioId(usuarioId);
        muestra.setEstado(EstadoDeMuestra.PENDIENTE);
        return enCurso(muestras.saveAndFlush(muestra));
    }

    @Override
    public List<FotoGuardada> fotos(UUID empresaId, UUID muestraId) {
        return fotos.deLaMuestra(empresaId, muestraId).stream().map(RepositorioDeMuestrasJpa::guardada).toList();
    }

    @Override
    public Optional<FotoGuardada> foto(UUID empresaId, UUID muestraId, int numero) {
        return fotos.deLaMuestra(empresaId, muestraId, numero).map(RepositorioDeMuestrasJpa::guardada);
    }

    /**
     * Borra la foto y lo manda a la base en seguida: el índice único de número por muestra no es diferible, así que
     * la foto que toma su número solo se puede insertar después.
     */
    @Override
    public void borrarFoto(UUID empresaId, UUID fotoId) {
        fotos.deLaEmpresa(empresaId, fotoId).ifPresent(foto -> {
            fotos.delete(foto);
            fotos.flush();
        });
    }

    @Override
    public FotoGuardada guardarFoto(UUID empresaId, UUID muestraId, int numero, String llave, FormatoDeFoto formato,
            long bytes) {
        FotoEntidad foto = new FotoEntidad();
        foto.setEmpresaId(empresaId);
        foto.setMuestraId(muestraId);
        foto.setNumero(numero);
        foto.setRuta(llave);
        foto.setFormato(formato);
        foto.setTamanoBytes(Math.toIntExact(bytes));
        foto.setEstado(EstadoDeFoto.CARGADA);
        return guardada(fotos.saveAndFlush(foto));
    }

    @Override
    public Optional<AvanceDelAnalisis> avance(UUID empresaId, UUID loteId) {
        return muestras.delLote(empresaId, loteId).stream().findFirst()
                .map(muestra -> AvanceDelAnalisis.de(muestra.getNumero(), muestra.getEstado(),
                        instante(muestra.getFechaEnvio()), instante(muestra.getFechaInicio()),
                        instante(muestra.getFechaFin())));
    }

    @Override
    public void enviar(UUID empresaId, UUID muestraId, UUID usuarioId, Instant fechaEnvio) {
        MuestraEntidad muestra = muestras.findById(muestraId)
                .filter(encontrada -> encontrada.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new IllegalStateException("La muestra " + muestraId + " no existe"));
        muestra.setEstado(EstadoDeMuestra.EN_ESPERA);
        muestra.setUsuarioId(usuarioId);
        muestra.setFechaEnvio(fecha(fechaEnvio));
        muestra.setFechaInicio(null);
        muestra.setFechaFin(null);
        muestras.saveAndFlush(muestra);
    }

    @Override
    public boolean terminar(MuestraTomada muestra, TerminoDeMuestra termino) {
        return muestras.terminar(muestra.empresaId(), muestra.id(), EstadoDeMuestra.EN_PROCESO,
                fecha(muestra.fechaInicio()), EstadoDeMuestra.TERMINADA, termino.modeloId(), termino.totalGranos(),
                termino.granosSanos(), termino.avisoConfianzaBaja(), termino.umbralConfianzaBaja(),
                termino.porcentajeMaxDudosos(), fecha(termino.fechaFin())) == 1;
    }

    @Override
    public boolean devolver(MuestraTomada muestra) {
        return muestras.cambiarEstado(muestra.empresaId(), muestra.id(), EstadoDeMuestra.EN_PROCESO,
                fecha(muestra.fechaInicio()), EstadoDeMuestra.PENDIENTE) == 1;
    }

    /**
     * Los guarda en el orden de la lista: Hibernate les da identificadores UUID v7 que crecen en ese orden y los
     * inserta en lotes al confirmar la transacción, sin volver a leer ninguno ({@link GranoEntidad}).
     */
    @Override
    public void guardarGranos(UUID empresaId, List<GranoDeFoto> lista) {
        granos.saveAll(lista.stream().map(grano -> {
            GranoEntidad entidad = new GranoEntidad();
            entidad.setEmpresaId(empresaId);
            entidad.setFotoId(grano.fotoId());
            entidad.setX(grano.x());
            entidad.setY(grano.y());
            entidad.setAncho(grano.ancho());
            entidad.setAlto(grano.alto());
            entidad.setClase(grano.clase());
            entidad.setConfianza(grano.confianza());
            return entidad;
        }).toList());
    }

    @Override
    public void aprobarFotos(UUID empresaId, UUID muestraId) {
        fotos.cambiarEstado(empresaId, muestraId, EstadoDeFoto.APROBADA);
    }

    @Override
    public List<ConteoDeMuestra> conteosTerminados(UUID empresaId, UUID loteId) {
        return muestras.terminadas(empresaId, loteId, EstadoDeMuestra.TERMINADA).stream()
                .map(FilaDeConteo::conteo)
                .toList();
    }

    @Override
    public List<MuestraGuardada> delLote(UUID empresaId, UUID loteId) {
        return muestras.conDetalle(empresaId, loteId).stream().map(FilaDeMuestra::guardada).toList();
    }

    @Override
    public List<GranoDeFoto> granos(UUID empresaId, Collection<UUID> fotoIds) {
        if (fotoIds.isEmpty()) {
            return List.of();
        }
        return granos.deLasFotos(empresaId, fotoIds).stream()
                .map(grano -> new GranoDeFoto(grano.getFotoId(), grano.getX(), grano.getY(), grano.getAncho(),
                        grano.getAlto(), grano.getClase(), grano.getConfianza()))
                .toList();
    }

    private static MuestraEnCurso enCurso(MuestraEntidad muestra) {
        return new MuestraEnCurso(muestra.getId(), muestra.getNumero(), muestra.getEstado());
    }

    private static FotoGuardada guardada(FotoEntidad foto) {
        return new FotoGuardada(foto.getId(), foto.getNumero(), foto.getRuta(), foto.getFormato(),
                foto.getTamanoBytes(), foto.getEstado());
    }

    private static OffsetDateTime fecha(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private static Instant instante(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.toInstant();
    }
}
