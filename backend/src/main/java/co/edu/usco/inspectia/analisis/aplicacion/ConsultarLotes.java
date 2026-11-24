/*
 * Nombre: ConsultarLotes.java
 * Descripción: Caso de uso de consultar los lotes de la empresa: el historial por páginas y con sus filtros, el
 *              detalle de un lote, quienes registraron lotes y las sugerencias de finca o proveedor (RF-04, RF-14,
 *              RNF-03).
 * Fecha de creación: 10/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import co.edu.usco.inspectia.comun.texto.Textos;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta los lotes de la empresa de la sesión (RF-14); los de otra empresa no existen para ella
 * (RNF-03). También sugiere las fincas y los proveedores que la empresa ya usó, para que al registrar un
 * lote se reutilice el nombre que ya existe (RF-04, restricción 3).
 */
@Service
@Transactional(readOnly = true)
public class ConsultarLotes {

    /** Cuántas fincas o proveedores se sugieren como máximo. */
    static final int MAXIMO_DE_SUGERENCIAS = 10;

    private final RepositorioDeLotes repositorio;

    /**
     * Crea el caso de uso; Spring le pasa el repositorio.
     *
     * @param repositorio los lotes guardados
     */
    public ConsultarLotes(RepositorioDeLotes repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Una página del historial con los lotes que cumplen los filtros, del último movimiento al más antiguo (ver
     * {@link RepositorioDeLotes#historial}).
     *
     * @param empresaId la empresa de la sesión
     * @param filtros los filtros (RF-14 C2); los que van en {@code null} no filtran
     * @param pagina número de la página, desde 0
     * @param tamano lotes por página
     * @return la página
     * @throws ReglaIncumplidaExcepcion si «Hasta» es anterior a «Resultado desde» (400 en {@code hasta}, RF-14
     *     restricción 5)
     */
    public Page<VistaDeLote> historial(UUID empresaId, FiltrosDelHistorial filtros, int pagina, int tamano) {
        if (filtros.fechasAlReves()) {
            throw new ReglaIncumplidaExcepcion(
                    List.of(new CampoInvalido("hasta", "lote.filtro.hasta_antes_de_desde")));
        }
        return repositorio.historial(empresaId, filtros, PageRequest.of(pagina, tamano));
    }

    /**
     * Quienes registraron lotes de la empresa, para el filtro «Usuario» del historial (RF-14 restricción 6): también
     * los desactivados, sin correos, ordenados por nombre sin distinguir tildes, mayúsculas ni los espacios del inicio
     * y del final, como las sugerencias de finca o proveedor.
     *
     * @param empresaId la empresa de la sesión
     * @return su identificador y su nombre
     */
    public List<Registrador> registradores(UUID empresaId) {
        return repositorio.registradores(empresaId).stream()
                .sorted(Comparator.comparing((Registrador registrador) -> Textos.claveDeBusqueda(registrador.nombre()))
                        .thenComparing(Registrador::nombre)
                        .thenComparing(Registrador::id))
                .toList();
    }

    /**
     * El detalle de un lote de la empresa.
     *
     * @param empresaId la empresa de la sesión
     * @param id el lote
     * @return el lote
     * @throws NoEncontradoExcepcion si no existe o es de otra empresa (404, RNF-03 C1)
     */
    public VistaDeLote detalle(UUID empresaId, UUID id) {
        return repositorio.buscar(empresaId, id).orElseThrow(() -> new NoEncontradoExcepcion("lote " + id));
    }

    /**
     * Las fincas y los proveedores de la empresa que contienen el texto, sin distinguir mayúsculas, tildes
     * ni los espacios del inicio y del final. Cada uno aparece una vez: si se escribió de varias formas, con
     * la más usada y, si empatan, con la más reciente. Así, una forma mal escrita una vez no reemplaza a la
     * que se usó bien muchas veces. Van en orden alfabético y son como máximo
     * {@value #MAXIMO_DE_SUGERENCIAS}.
     *
     * @param empresaId la empresa de la sesión
     * @param texto lo que el usuario lleva escrito; vacío para no filtrar
     * @return las sugerencias
     */
    public List<String> origenes(UUID empresaId, String texto) {
        String buscado = Textos.claveDeBusqueda(texto == null ? "" : texto);
        Map<String, OrigenUsado> formaPorClave = new HashMap<>();
        for (OrigenUsado usado : repositorio.origenesUsados(empresaId)) {
            formaPorClave.merge(Textos.claveDeBusqueda(usado.origen()), usado,
                    (una, otra) -> otra.preferidaSobre(una) ? otra : una);
        }
        return formaPorClave.entrySet().stream()
                .filter(entrada -> entrada.getKey().contains(buscado))
                .sorted(Map.Entry.comparingByKey())
                .limit(MAXIMO_DE_SUGERENCIAS)
                .map(entrada -> entrada.getValue().origen())
                .toList();
    }
}
