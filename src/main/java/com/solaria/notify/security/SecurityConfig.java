package com.solaria.notify.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Duas SecurityFilterChain independentes, segregadas por @Order.
 *
 * @Order(1): /internal/** -> X-Service-Key (serviços do cluster, nunca via Kong).
 * @Order(2): demais rotas -> JWT de usuário do api-auth (resource server RS256/JWKS).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({ JwtProperties.class, ServiceKeyProperties.class })
public class SecurityConfig {

    private final JwtProperties jwtProperties;
    private final ProblemDetailAuthenticationEntryPoint problemDetailAuthenticationEntryPoint;
    private final ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtProperties jwtProperties,
            ProblemDetailAuthenticationEntryPoint problemDetailAuthenticationEntryPoint,
            ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler) {
        this.jwtProperties = jwtProperties;
        this.problemDetailAuthenticationEntryPoint = problemDetailAuthenticationEntryPoint;
        this.problemDetailAccessDeniedHandler = problemDetailAccessDeniedHandler;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurityFilterChain(HttpSecurity http,
            ServiceKeyAuthFilter serviceKeyAuthFilter) throws Exception {
        http
                .securityMatcher("/internal/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // autentica pela chave de serviço antes do filtro anônimo
                .addFilterBefore(serviceKeyAuthFilter, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("SERVICE"))
                // quem não é serviço recebe 401 (não 403), mesmo se autenticado de outra forma
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(problemDetailAuthenticationEntryPoint)
                        .accessDeniedHandler((request, response, ex) -> problemDetailAuthenticationEntryPoint
                                .commence(request, response, new InsufficientAuthenticationException(
                                        "Chave de serviço ausente ou inválida", ex))));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(userJwtDecoder()))
                        // oauth2ResourceServer() tem prioridade sobre exceptionHandling
                        .authenticationEntryPoint(problemDetailAuthenticationEntryPoint)
                        .accessDeniedHandler(problemDetailAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/docs", "/swagger-ui/**", "/openapi.json/**", "/openapi.json.yaml")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(problemDetailAuthenticationEntryPoint)
                        .accessDeniedHandler(problemDetailAccessDeniedHandler));
        return http.build();
    }

    // O filtro só roda dentro da chain interna, não como filtro global do servlet
    @Bean
    public FilterRegistrationBean<ServiceKeyAuthFilter> serviceKeyAuthFilterRegistration(
            ServiceKeyAuthFilter filter) {
        FilterRegistrationBean<ServiceKeyAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public JwtDecoder userJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwtProperties.getJwkSetUri()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer()),
                new AccessTokenTypeValidator()));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "apikey"));
        configuration.setExposedHeaders(List.of("X-Trace-Id"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
