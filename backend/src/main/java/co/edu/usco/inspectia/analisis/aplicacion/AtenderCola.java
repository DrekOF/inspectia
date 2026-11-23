/*
 * Nombre: AtenderCola.java
 * Descripción: Caso de uso que atiende la siguiente muestra de la cola de análisis: la toma, lee sus fotos, las
 *              clasifica fuera de toda transacción y guarda el resultado; si algo falla, la devuelve a pendiente
 *              (RNF-02 C3, RF-08, RF-10).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Atiende la cola de análisis, una muestra a la vez (RNF-02 C3). No tiene transacción propia: tomar la muestra
 * ({@link ColaDeAnalisis}, transacción 1) y guardar su análisis ({@link GuardarAnalisis}, transacción 2) son cortas, y
 * entre las dos se leen las fotos y se clasifican sin nada abierto en la base, aunque el clasificador tarde (RNF-01).
 *
 * <p>Si leer una foto, clasificar o guardar falla, la muestra vuelve a pendiente sin resultado y el error queda en el
 * registro del servidor (RF-08; specs/modelo-datos.md, muestra). No se reintenta sola: el usuario la envía de nuevo.
 * Lo llama el trabajador de la cola y, en las pruebas de integración, la prueba misma.
 */
@Slf4j
@Service
public class AtenderCola {

    private final ColaDeAnalisis cola;
    private final RepositorioDeMuestras muestras;
    private final AlmacenDeArchivos almacen;
    private final ClasificadorDeGranos clasificador;
    private final GuardarAnalisis guardar;
    private final Clock reloj;

    /**
     * Crea el caso de uso; Spring le pasa sus dependencias.
     *
     * @param cola la cola de análisis
     * @param muestras las muestras y sus fotos
     * @param almacen el almacén de los archivos, de donde se leen las fotos
     * @param clasificador el clasificador de granos
     * @param guardar la transacción que guarda el análisis o devuelve la muestra
     * @param reloj el reloj de la aplicación, en UTC
     */
    public AtenderCola(ColaDeAnalisis cola, RepositorioDeMuestras muestras, AlmacenDeArchivos almacen,
            ClasificadorDeGranos clasificador, GuardarAnalisis guardar, Clock reloj) {
        this.cola = cola;
        this.muestras = muestras;
        this.almacen = almacen;
        this.clasificador = clasificador;
        this.guardar = guardar;
        this.reloj = reloj;
    }

    /**
     * Atiende la muestra que lleva más tiempo en espera, si hay alguna.
     *
     * @return {@code true} si tomó una muestra, aunque su análisis haya fallado; {@code false} si la cola está vacía
     */
    public boolean atenderUna() {
        Optional<MuestraTomada> tomada = cola.tomarLaSiguiente(reloj.instant().truncatedTo(ChronoUnit.MICROS));
        tomada.ifPresent(this::atender);
        return tomada.isPresent();
    }

    private void atender(MuestraTomada muestra) {
        try {
            List<FotoParaClasificar> fotos = muestras.fotos(muestra.empresaId(), muestra.id()).stream()
                    .map(foto -> new FotoParaClasificar(foto.numero(), foto.formato(), almacen.leer(foto.llave())))
                    .toList();
            GuardarAnalisis.Desenlace desenlace = guardar.guardar(muestra, clasificador.clasificar(fotos));
            log.info("Análisis de la muestra {} del lote {}: {}", muestra.id(), muestra.loteId(), desenlace);
        } catch (RuntimeException excepcion) {
            log.error("El análisis de la muestra {} del lote {} falló: vuelve a pendiente, sin resultado", muestra.id(),
                    muestra.loteId(), excepcion);
            devolver(muestra);
        }
    }

    /** Si tampoco se puede devolver, la muestra queda «en proceso» y vuelve a la cola al arrancar el servidor. */
    private void devolver(MuestraTomada muestra) {
        try {
            guardar.devolver(muestra);
        } catch (RuntimeException excepcion) {
            log.error("No se pudo devolver a pendiente la muestra {}: vuelve a la cola al arrancar el servidor",
                    muestra.id(), excepcion);
        }
    }
}
