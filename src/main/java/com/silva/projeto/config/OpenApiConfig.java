package com.silva.projeto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Documentação da API, publicada em /swagger-ui.html (tela) e /v3/api-docs (JSON). */
@Configuration
public class OpenApiConfig {

    static final String ESQUEMA_TOKEN = "token";

    @Bean
    public OpenAPI documentacaoDaApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Prontê Conveniência - API")
                        .version("1.0.0")
                        .description("API do sistema de vendas usada pelos fronts web, desktop e mobile. "
                                + "Para testar as rotas: faça login em POST /auth/login, clique em "
                                + "Authorize e cole o token recebido."))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_TOKEN, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token devolvido por POST /auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_TOKEN));
    }
}
