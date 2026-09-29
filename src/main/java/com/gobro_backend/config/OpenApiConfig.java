package com.gobro_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger UI configuration (springdoc-openapi).
 * Exposes the documented API at /swagger-ui.html and /v3/api-docs,
 * with a global JWT Bearer security scheme so protected endpoints
 * can be tested directly from the UI.
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.name:GoBro Platform}")
    private String appName;

    @Value("${app.api.version:v1}")
    private String apiVersion;

    @Value("${app.api.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${app.api.prod-url:https://api.gobro.tn}")
    private String prodUrl;

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        Server devServer = new Server()
                .url(devUrl)
                .description("Serveur de développement");

        Server prodServer = new Server()
                .url(prodUrl)
                .description("Serveur de production");

        Contact contact = new Contact()
                .name("Équipe GoBro")
                .email("contact@gobro.tn")
                .url("https://gobro.tn");

        Info info = new Info()
                .title(appName + " API")
                .version(apiVersion)
                .description("API REST de la plateforme tunisienne d'apprentissage en informatique : "
                        + "authentification, cours, quiz, examens, certificats, paiement (D17, Konnect, "
                        + "Flouci), gamification (XP, niveaux, badges, défis) et administration.")
                .contact(contact)
                .license(new License().name("Propriétaire").url("https://gobro.tn/licence"));

        SecurityScheme bearerScheme = new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Authentification via JWT. Format: Bearer {token}");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}