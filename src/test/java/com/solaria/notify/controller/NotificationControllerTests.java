package com.solaria.notify.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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

import com.solaria.notify.dto.response.NotificationResponseDTO;
import com.solaria.notify.dto.response.PageResponseDTO;
import com.solaria.notify.dto.response.UnreadCountResponseDTO;
import com.solaria.notify.exception.handler.GlobalExceptionHandler;
import com.solaria.notify.exception.handler.ProblemDetailFactory;
import com.solaria.notify.model.enums.NotificationType;
import com.solaria.notify.security.ProblemDetailAccessDeniedHandler;
import com.solaria.notify.security.ProblemDetailAuthenticationEntryPoint;
import com.solaria.notify.security.SecurityConfig;
import com.solaria.notify.security.ServiceKeyAuthFilter;
import com.solaria.notify.service.NotificationService;

@WebMvcTest(NotificationController.class)
@Import({ SecurityConfig.class, ServiceKeyAuthFilter.class, ProblemDetailAuthenticationEntryPoint.class,
        ProblemDetailAccessDeniedHandler.class, ProblemDetailFactory.class, GlobalExceptionHandler.class })
@TestPropertySource(properties = {
        "app.service-keys.api-core=unit-test-service-key-0123456789abcdef",
        "app.cors.allowed-origins=http://localhost:3000",
        "app.jwt.jwk-set-uri=http://localhost:8081/.well-known/jwks.json",
        "app.jwt.issuer=solaria-auth" })
class NotificationControllerTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void listReturnsPage() throws Exception {
        NotificationResponseDTO item = new NotificationResponseDTO(UUID.randomUUID(), NotificationType.NEW_MESSAGE,
                "t", "b", Map.of("k", "v"), Instant.parse("2026-01-10T12:00:00Z"), null);
        when(notificationService.list(0, 20, false)).thenReturn(new PageResponseDTO<>(List.of(item), 0, 20, 1));

        mockMvc.perform(get("/notify/notifications").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("NEW_MESSAGE"))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void negativePageIs400() throws Exception {
        mockMvc.perform(get("/notify/notifications").param("page", "-1").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verify(notificationService, never()).list(anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void zeroSizeIs400() throws Exception {
        mockMvc.perform(get("/notify/notifications").param("size", "0").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verify(notificationService, never()).list(anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void unreadCountReturnsCount() throws Exception {
        when(notificationService.unreadCount()).thenReturn(new UnreadCountResponseDTO(3));

        mockMvc.perform(get("/notify/notifications/unread-count").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void markReadReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificationService.markRead(id)).thenReturn(new NotificationResponseDTO(id, NotificationType.NEW_MESSAGE,
                "t", "b", Map.of(), Instant.parse("2026-01-10T12:00:00Z"), Instant.parse("2026-01-10T12:05:00Z")));

        mockMvc.perform(patch("/notify/notifications/" + id + "/read").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void markReadWithInvalidUuidIs400() throws Exception {
        mockMvc.perform(patch("/notify/notifications/not-a-uuid/read").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/notify/notifications/not-a-uuid/read"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("not-a-uuid\""))));
        verify(notificationService, never()).markRead(any());
    }

    @Test
    void readAllReturns204() throws Exception {
        mockMvc.perform(patch("/notify/notifications/read-all").with(jwt()))
                .andExpect(status().isNoContent());
        verify(notificationService).markAllRead();
    }
}
