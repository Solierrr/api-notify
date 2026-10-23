package com.solaria.notify.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.solaria.notify.dto.response.DeviceResponseDTO;
import com.solaria.notify.exception.handler.GlobalExceptionHandler;
import com.solaria.notify.exception.handler.ProblemDetailFactory;
import com.solaria.notify.model.enums.Platform;
import com.solaria.notify.security.ProblemDetailAccessDeniedHandler;
import com.solaria.notify.security.ProblemDetailAuthenticationEntryPoint;
import com.solaria.notify.security.SecurityConfig;
import com.solaria.notify.security.ServiceKeyAuthFilter;
import com.solaria.notify.service.DeviceService;

@WebMvcTest(DeviceController.class)
@Import({ SecurityConfig.class, ServiceKeyAuthFilter.class, ProblemDetailAuthenticationEntryPoint.class,
        ProblemDetailAccessDeniedHandler.class, ProblemDetailFactory.class, GlobalExceptionHandler.class })
@TestPropertySource(properties = {
        "app.service-keys.api-core=k1",
        "app.cors.allowed-origins=http://localhost:3000",
        "app.jwt.jwk-set-uri=http://localhost:8081/.well-known/jwks.json",
        "app.jwt.issuer=solaria-auth" })
class DeviceControllerTests {

    private static final String BODY = "{\"fcmToken\":\"tok\",\"platform\":\"ANDROID\",\"appVersion\":\"1.0.0\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DeviceService deviceService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void putReturns200WithBody() throws Exception {
        when(deviceService.register(eq("fid-1"), any()))
                .thenReturn(new DeviceResponseDTO("fid-1", Platform.ANDROID, "1.0.0", Instant.parse("2026-01-10T12:00:00Z")));

        mockMvc.perform(put("/notify/devices/fid-1").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installationId").value("fid-1"))
                .andExpect(jsonPath("$.platform").value("ANDROID"));
    }

    @Test
    void putWithBlankTokenIs400() throws Exception {
        mockMvc.perform(put("/notify/devices/fid-1").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fcmToken\":\" \",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void putWithInvalidInstallationIdIs400() throws Exception {
        mockMvc.perform(put("/notify/devices/" + "a".repeat(65)).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(put("/notify/devices/a.b").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verify(deviceService, never()).register(any(), any());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/notify/devices/fid-1").with(jwt()))
                .andExpect(status().isNoContent());
        verify(deviceService).remove("fid-1");
    }
}
