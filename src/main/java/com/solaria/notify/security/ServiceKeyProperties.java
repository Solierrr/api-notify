package com.solaria.notify.security;

import java.util.LinkedHashMap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Chaves de serviço (header X-Service-Key): nome do serviço -> chave.
 * O próprio bean é o mapa, para app.service-keys.api-core=k virar {"api-core": "k"}.
 */
@ConfigurationProperties(prefix = "app.service-keys")
public class ServiceKeyProperties extends LinkedHashMap<String, String> {
}
