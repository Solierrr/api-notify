package com.solaria.notify.security;

import java.util.LinkedHashMap;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Chaves de serviço (header X-Service-Key): nome do serviço -> chave.
 * O próprio bean é o mapa, para app.service-keys.api-core=k virar {"api-core": "k"}.
 */
@ConfigurationProperties(prefix = "app.service-keys")
public class ServiceKeyProperties extends LinkedHashMap<String, String> {

    static final int MIN_KEY_LENGTH = 32;

    // Falha no startup se alguma chave configurada for fraca (nunca inclui o valor na mensagem)
    @PostConstruct
    public void validate() {
        forEach((service, key) -> {
            if (key != null && !key.isBlank() && key.length() < MIN_KEY_LENGTH) {
                throw new IllegalStateException("A chave de serviço de '" + service + "' (app.service-keys."
                        + service + ") deve ter pelo menos " + MIN_KEY_LENGTH + " caracteres");
            }
        });
    }
}
