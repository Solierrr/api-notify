package com.solaria.notify.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.solaria.notify.model.enums.NotificationType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationResponseDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Id da notificação", example = "3f2b8c1e-5d4a-4e9b-9c7d-1a2b3c4d5e6f")
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Tipo da notificação")
    private NotificationType type;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Título exibido no push", example = "Nova mensagem")
    private String title;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Texto exibido no push", example = "Você recebeu uma nova mensagem no chat.")
    private String body;
    @Schema(description = "Dados extras enviados junto do push", example = "{\"chatId\": \"42\"}")
    private Map<String, String> data;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Momento em que a notificação foi gravada", example = "2026-10-24T13:45:00Z")
    private Instant createdAt;
    @Schema(description = "Momento em que foi marcada como lida; nulo se ainda não lida", example = "2026-10-24T14:02:10Z")
    private Instant readAt;
}
