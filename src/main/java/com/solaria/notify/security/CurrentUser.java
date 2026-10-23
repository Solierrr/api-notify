package com.solaria.notify.security;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Acesso à identidade autenticada: authId do usuário (JWT) ou nome do serviço (chain interna).
 */
@Component
public class CurrentUser {

    public UUID authId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return UUID.fromString(jwt.getToken().getSubject());
        }
        throw new AuthenticationCredentialsNotFoundException("Usuário não autenticado");
    }

    public String serviceName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String name) {
            return name;
        }
        throw new AuthenticationCredentialsNotFoundException("Serviço não autenticado");
    }
}
