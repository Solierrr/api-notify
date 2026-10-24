package com.solaria.notify.dto.request;

import java.util.Map;
import java.util.UUID;

import com.solaria.notify.dto.validation.FcmPayloadSize;
import com.solaria.notify.dto.validation.NotReservedDataKey;
import com.solaria.notify.model.enums.NotificationType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@FcmPayloadSize
public class InternalNotificationRequestDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Id da pessoa destinatária no api-auth",
            example = "8d6f2a4b-1c3e-4f5a-9b7c-2d4e6f8a0b1c")
    @NotNull(message = "authId é obrigatório")
    private UUID authId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Tipo da notificação")
    @NotNull(message = "type é obrigatório")
    private NotificationType type;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Título exibido no push", example = "Nova mensagem")
    @NotBlank(message = "title é obrigatório")
    @Size(max = 120, message = "title deve ter no máximo 120 caracteres")
    private String title;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Texto exibido no push", example = "Você recebeu uma nova mensagem no chat.")
    @NotBlank(message = "body é obrigatório")
    @Size(max = 500, message = "body deve ter no máximo 500 caracteres")
    private String body;

    @Schema(description = "Dados extras entregues ao app junto do push (até 20 chaves; chaves reservadas do FCM "
            + "e dos campos fixos são recusadas)", example = "{\"chatId\": \"42\"}")
    @Size(max = 20, message = "data deve ter no máximo 20 entradas")
    private Map<
            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,63}$", message = "chave de data inválida") @NotReservedDataKey String,
            @NotNull(message = "valor de data não pode ser nulo") @Size(max = 512, message = "valor de data deve ter no máximo 512 caracteres") String> data;
}
