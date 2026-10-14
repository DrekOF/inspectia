/*
 * Nombre: ConfiguracionOpenApi.java
 * Descripción: Datos generales de la documentación OpenAPI: título, versión y el token Bearer que
 *              pide toda la API (RP-04, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Datos generales de la documentación OpenAPI (RP-04), la que muestra Swagger UI en
 * {@code /swagger-ui.html}.
 *
 * <p>Toda la API pide el token en el encabezado {@code Authorization: Bearer} (D-28), salvo las
 * operaciones marcadas con {@code @SecurityRequirements} vacío, como iniciar sesión. En Swagger UI,
 * el botón «Authorize» recibe el token que entrega {@code POST /api/v1/sesion}.
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(title = "API de InspectIA", version = "v1",
                description = "API REST de InspectIA: sesión, empresas y, más adelante, usuarios, lotes, análisis, "
                        + "reportes y estadísticas. Intercambia JSON; los errores van en application/problem+json."),
        security = @SecurityRequirement(name = ConfiguracionOpenApi.TOKEN))
@SecurityScheme(name = ConfiguracionOpenApi.TOKEN, type = SecuritySchemeType.HTTP, scheme = "bearer",
        bearerFormat = "JWT",
        description = "El token que entrega POST /api/v1/sesion. Vence a las 8 horas; después hay que iniciar sesión "
                + "de nuevo.")
public class ConfiguracionOpenApi {

    /** Nombre del esquema de seguridad del token en la documentación. */
    public static final String TOKEN = "token";
}
