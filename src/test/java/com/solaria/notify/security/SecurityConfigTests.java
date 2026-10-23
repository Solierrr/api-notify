package com.solaria.notify.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.solaria.notify.exception.handler.ProblemDetailFactory;

@WebMvcTest(SecurityConfigTests.PingController.class)
@Import({ SecurityConfigTests.PingController.class, SecurityConfig.class, ServiceKeyAuthFilter.class, ProblemDetailAuthenticationEntryPoint.class,
        ProblemDetailAccessDeniedHandler.class, ProblemDetailFactory.class })
@TestPropertySource(properties = {
        "app.service-keys.api-core=unit-test-service-key-0123456789abcdef",
        "app.cors.allowed-origins=http://localhost:3000",
        "app.jwt.jwk-set-uri=http://localhost:8081/.well-known/jwks.json",
        "app.jwt.issuer=solaria-auth" })
class SecurityConfigTests {

    @RestController
    static class PingController {
        @GetMapping("/internal/ping")
        String internal() {
            return "ok";
        }

        @GetMapping("/notify/ping")
        String user() {
            return "ok";
        }
    }

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void internalWithoutKeyIs401() throws Exception {
        mockMvc.perform(get("/internal/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void internalWithValidKeyIs200() throws Exception {
        mockMvc.perform(get("/internal/ping").header("X-Service-Key", "unit-test-service-key-0123456789abcdef"))
                .andExpect(status().isOk());
    }

    @Test
    void internalWithBlankKeyIs401() throws Exception {
        mockMvc.perform(get("/internal/ping").header("X-Service-Key", ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userRouteWithoutJwtIs401() throws Exception {
        mockMvc.perform(get("/notify/ping")).andExpect(status().isUnauthorized());
    }

    @Test
    void userRouteWithJwtIs200() throws Exception {
        mockMvc.perform(get("/notify/ping")
                .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isOk());
    }

    @Test
    void internalRouteRejectsUserJwt() throws Exception {
        mockMvc.perform(get("/internal/ping")
                .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isUnauthorized());
    }
}
