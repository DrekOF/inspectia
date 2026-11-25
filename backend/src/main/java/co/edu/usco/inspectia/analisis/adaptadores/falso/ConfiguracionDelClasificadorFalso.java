/*
 * Nombre: ConfiguracionDelClasificadorFalso.java
 * Descripción: Arma el clasificador falso y registra su fila en la tabla modelo al arrancar, solo si está activo
 *              (inspectia.ia.clasificador=falso) (RF-08, D-39).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.falso;

import co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeModelos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El clasificador falso ({@link ClasificadorFalso}) y, con él, la revisión de las fotos que no revisa
 * ({@link SinRevisionDeFotos}). Se carga con {@code inspectia.ia.clasificador=falso}: la
 * condición no tiene valor por defecto, pero application.yml trae «falso» mientras no esté el servicio de IA (PR 4).
 * El PR del despliegue exigirá en AWS un valor explícito, para que el falso no llegue a producción sin que alguien lo
 * pida.
 *
 * <p>Al arrancar, registra la fila de su modelo en {@code modelo} si no existe ({@code INSERT … ON CONFLICT (nombre,
 * version) DO NOTHING}), con los recall de EfficientNet-B0, sin las demás métricas y sin ponerlo en uso. No va en una
 * migración: en producción dejaría el falso cargado (specs/modelo-datos.md, modelo). Se registra después de crear todos
 * los beans y antes de que arranque el trabajador de la cola, así ninguna muestra se analiza sin su modelo.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "inspectia.ia", name = "clasificador", havingValue = "falso")
@EnableConfigurationProperties(PropiedadesDelClasificadorFalso.class)
public class ConfiguracionDelClasificadorFalso {

    /**
     * El clasificador falso.
     *
     * @param propiedades su demora
     * @return el clasificador
     */
    @Bean
    ClasificadorFalso clasificadorFalso(PropiedadesDelClasificadorFalso propiedades) {
        log.warn("El análisis usa el clasificador falso (inspectia.ia.clasificador=falso): sus resultados no son "
                + "reales");
        return new ClasificadorFalso(propiedades.demora());
    }

    /**
     * Sin revisión rápida de las fotos: con el clasificador falso, la foto confirmada queda «Cargada» (D-46).
     *
     * @return el revisor que no revisa
     */
    @Bean
    SinRevisionDeFotos sinRevisionDeFotos() {
        return new SinRevisionDeFotos();
    }

    /**
     * Registra el modelo del clasificador falso en la tabla {@code modelo}, si falta.
     *
     * @param modelos los modelos registrados
     * @return el registro, que corre después de crear todos los beans
     */
    @Bean
    SmartInitializingSingleton registroDelModeloFalso(RepositorioDeModelos modelos) {
        return () -> modelos.registrarSiFalta(ClasificadorFalso.MODELO.nombre(), ClasificadorFalso.MODELO.version(),
                ClasificadorFalso.RECALLS);
    }
}
