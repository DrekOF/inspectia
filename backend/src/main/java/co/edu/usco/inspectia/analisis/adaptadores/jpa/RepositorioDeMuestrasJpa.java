/*
 * Nombre: RepositorioDeMuestrasJpa.java
 * Descripción: El puerto RepositorioDeMuestras con JPA, sobre las tablas muestra y foto (RF-05, RNF-03).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.aplicacion.FotoGuardada;
import co.edu.usco.inspectia.analisis.aplicacion.MuestraEnCurso;
import co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeMuestras;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** El puerto {@link RepositorioDeMuestras} con JPA, sobre las tablas {@code muestra} y {@code foto}. */
@Component
public class RepositorioDeMuestrasJpa implements RepositorioDeMuestras {

    private final MuestrasJpa muestras;
    private final FotosJpa fotos;

    /**
     * Crea el adaptador; Spring le pasa los repositorios de Spring Data.
     *
     * @param muestras la tabla {@code muestra}
     * @param fotos la tabla {@code foto}
     */
    public RepositorioDeMuestrasJpa(MuestrasJpa muestras, FotosJpa fotos) {
        this.muestras = muestras;
        this.fotos = fotos;
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

    private static MuestraEnCurso enCurso(MuestraEntidad muestra) {
        return new MuestraEnCurso(muestra.getId(), muestra.getNumero(), muestra.getEstado());
    }

    private static FotoGuardada guardada(FotoEntidad foto) {
        return new FotoGuardada(foto.getId(), foto.getNumero(), foto.getRuta(), foto.getFormato(),
                foto.getTamanoBytes(), foto.getEstado());
    }
}
