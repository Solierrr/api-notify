package com.solaria.notify.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI solariaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Solaria/api-notify")
                        .description("API Spring de notificações push do Solaria.")
                        .version("v1"));
    }
}
