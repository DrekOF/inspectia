/*
 * Nombre: DocumentacionDeLotes.java
 * Descripción: El ejemplo de la fecha de recepción en la documentación OpenAPI de los lotes: la fecha de hace una
 *              semana en Colombia, para que el cuerpo de ejemplo de Swagger UI siempre se pueda registrar (RF-04,
 *              RP-04).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import io.swagger.v3.oas.models.media.Schema;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El ejemplo de la fecha de recepción en la documentación OpenAPI (RP-04). Una fecha fija dejaría de valer al año,
 * cuando quedara de hace más de un año (RF-04, restricción 4). Sin ejemplo, Swagger UI pondría la fecha de hoy en
 * UTC, que de 7 a 12 de la noche en Colombia ya es mañana (C4). Por eso el ejemplo es la fecha de hace una semana
 * en Colombia, contada al armar la documentación.
 */
@Configuration(proxyBeanMethods = false)
public class DocumentacionDeLotes {

    /** «Hoy» es el día en Colombia, como en el registro de lotes. */
    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    /**
     * Pone el ejemplo de la fecha de recepción en el esquema de la solicitud de registro.
     *
     * @param reloj el reloj de la aplicación, en UTC
     * @return el ajuste de la documentación
     */
    @Bean
    OpenApiCustomizer ejemploDeLaFechaDeRecepcion(Clock reloj) {
        return documento -> {
            Map<String, Schema> esquemas = documento.getComponents() == null ? null
                    : documento.getComponents().getSchemas();
            Schema<?> solicitud = esquemas == null ? null : esquemas.get("LoteSolicitud");
            if (solicitud == null || solicitud.getProperties() == null) {
                return;
            }
            Schema<?> fecha = solicitud.getProperties().get("fechaRecepcion");
            if (fecha != null) {
                fecha.setExample(LocalDate.ofInstant(reloj.instant(), COLOMBIA).minusWeeks(1).toString());
            }
        };
    }
}
