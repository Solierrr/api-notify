package com.solaria.notify.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CurrentUserTests {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static UsernamePasswordAuthenticationToken serviceToken() {
        return new UsernamePasswordAuthenticationToken("api-core", null,
                List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
    }

    private static JwtAuthenticationToken jwtToken(UUID sub) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    void serviceNameReturnsNameForServiceToken() {
        SecurityContextHolder.getContext().setAuthentication(serviceToken());
        assertThat(currentUser.serviceName()).isEqualTo("api-core");
    }

    @Test
    void serviceNameRejectsJwt() {
        SecurityContextHolder.getContext().setAuthentication(jwtToken(UUID.randomUUID()));
        assertThatThrownBy(currentUser::serviceName).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void serviceNameRejectsAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("k", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThatThrownBy(currentUser::serviceName).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void serviceNameRejectsUsernamePasswordWithoutServiceRole() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("x", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        assertThatThrownBy(currentUser::serviceName).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void authIdRejectsServiceToken() {
        SecurityContextHolder.getContext().setAuthentication(serviceToken());
        assertThatThrownBy(currentUser::authId).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void authIdReturnsJwtSubject() {
        UUID id = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(jwtToken(id));
        assertThat(currentUser.authId()).isEqualTo(id);
    }
}
