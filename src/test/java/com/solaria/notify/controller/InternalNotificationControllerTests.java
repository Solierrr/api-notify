package com.solaria.notify.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

import com.solaria.notify.dto.response.NotificationDispatchResponseDTO;
import com.solaria.notify.exception.handler.GlobalExceptionHandler;
import com.solaria.notify.exception.handler.ProblemDetailFactory;
import com.solaria.notify.security.ProblemDetailAccessDeniedHandler;
import com.solaria.notify.security.ProblemDetailAuthenticationEntryPoint;
import com.solaria.notify.security.SecurityConfig;
import com.solaria.notify.security.ServiceKeyAuthFilter;
import com.solaria.notify.service.NotificationService;

@WebMvcTest(InternalNotificationController.class)
@Import({ SecurityConfig.class, ServiceKeyAuthFilter.class, ProblemDetailAuthenticationEntryPoint.class,
        ProblemDetailAccessDeniedHandler.class, ProblemDetailFactory.class, GlobalExceptionHandler.class })
@TestPropertySource(properties = {
        "app.service-keys.api-core=unit-test-service-key-0123456789abcdef",
        "app.cors.allowed-origins=http://localhost:3000",
        "app.jwt.jwk-set-uri=http://localhost:8081/.well-known/jwks.json",
        "app.jwt.issuer=solaria-auth" })
class InternalNotificationControllerTests {

    private static final String URL = "/internal/notifications";
    private static final String KEY = "unit-test-service-key-0123456789abcdef";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private static String body(String type, String title) {
        return "{\"authId\":\"" + UUID.randomUUID() + "\",\"type\":\"" + type + "\",\"title\":\"" + title
                + "\",\"body\":\"corpo\",\"data\":{\"chatId\":\"1\"}}";
    }

    @Test
    void dispatchWithKeyReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificationService.dispatch(any())).thenReturn(new NotificationDispatchResponseDTO(id, 1, 0, 0));

        mockMvc.perform(post(URL).header("X-Service-Key", "unit-test-service-key-0123456789abcdef").contentType(MediaType.APPLICATION_JSON)
                .content(body("NEW_MESSAGE", "Oi")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.sent").value(1))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.removedDevices").value(0));
    }

    @Test
    void dispatchWithoutKeyIs401() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("NEW_MESSAGE", "Oi")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verify(notificationService, never()).dispatch(any());
    }

    @Test
    void dispatchWithUnknownTypeIs400() throws Exception {
        mockMvc.perform(post(URL).header("X-Service-Key", "unit-test-service-key-0123456789abcdef").contentType(MediaType.APPLICATION_JSON)
                .content(body("FOO", "Oi")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verify(notificationService, never()).dispatch(any());
    }

    @Test
    void dispatchWithBlankTitleIs400() throws Exception {
        mockMvc.perform(post(URL).header("X-Service-Key", "unit-test-service-key-0123456789abcdef").contentType(MediaType.APPLICATION_JSON)
                .content(body("NEW_MESSAGE", " ")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verify(notificationService, never()).dispatch(any());
    }

    private static String bodyWithData(String dataJson) {
        return "{\"authId\":\"" + UUID.randomUUID()
                + "\",\"type\":\"NEW_MESSAGE\",\"title\":\"Oi\",\"body\":\"corpo\",\"data\":" + dataJson + "}";
    }

    private void expectDataRejected(String dataJson) throws Exception {
        mockMvc.perform(post(URL).header("X-Service-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                .content(bodyWithData(dataJson)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verify(notificationService, never()).dispatch(any());
    }

    @Test
    void reservedDataKeyIs400() throws Exception {
        expectDataRejected("{\"from\":\"x\"}");
        expectDataRejected("{\"title\":\"x\"}");
        expectDataRejected("{\"notificationId\":\"x\"}");
        expectDataRejected("{\"collapse_key\":\"x\"}");
    }

    @Test
    void googleAndGcmPrefixedDataKeyIs400() throws Exception {
        expectDataRejected("{\"google_x\":\"x\"}");
        expectDataRejected("{\"Google_x\":\"x\"}");
        expectDataRejected("{\"gcm_x\":\"x\"}");
    }

    @Test
    void malformedDataKeyIs400() throws Exception {
        expectDataRejected("{\"google.x\":\"x\"}");
        expectDataRejected("{\"1abc\":\"x\"}");
        expectDataRejected("{\"" + "a".repeat(65) + "\":\"x\"}");
    }

    @Test
    void tooLongDataValueIs400() throws Exception {
        expectDataRejected("{\"chatId\":\"" + "v".repeat(513) + "\"}");
    }

    @Test
    void nullDataValueIs400() throws Exception {
        expectDataRejected("{\"chatId\":null}");
    }

    @Test
    void validDataMapReturns201() throws Exception {
        when(notificationService.dispatch(any()))
                .thenReturn(new NotificationDispatchResponseDTO(UUID.randomUUID(), 0, 0, 0));
        mockMvc.perform(post(URL).header("X-Service-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                .content(bodyWithData("{\"chatId\":\"" + "v".repeat(512) + "\",\"a_1\":\"2\"}")))
                .andExpect(status().isCreated());
    }
}
