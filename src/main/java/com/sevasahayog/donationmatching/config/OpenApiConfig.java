package com.sevasahayog.donationmatching.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI donationMatchingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Donation Matching Portal API")
                        .description("""
                                REST API for the Seva Sahayog Foundation donation matching platform.

                                The platform connects donors with receivers and lets administrators review
                                and approve suggested matches. Register to obtain a JWT, then use the
                                Authorize button to call protected endpoints.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Seva Sahayog Foundation"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
