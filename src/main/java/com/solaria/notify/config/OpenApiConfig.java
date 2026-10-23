package com.solaria.notify.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI solierOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Solier/api-notify")
                        .description("API Spring de notificações push do Solier.")
                        .version("v1"));
    }
}
