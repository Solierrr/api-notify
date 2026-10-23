package com.solaria.notify.dto.request;

import java.util.Map;
import java.util.UUID;

import com.solaria.notify.dto.validation.FcmPayloadSize;
import com.solaria.notify.dto.validation.NotReservedDataKey;
import com.solaria.notify.model.enums.NotificationType;

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

    @NotNull(message = "authId é obrigatório")
    private UUID authId;

    @NotNull(message = "type é obrigatório")
    private NotificationType type;

    @NotBlank(message = "title é obrigatório")
    @Size(max = 120, message = "title deve ter no máximo 120 caracteres")
    private String title;

    @NotBlank(message = "body é obrigatório")
    @Size(max = 500, message = "body deve ter no máximo 500 caracteres")
    private String body;

    @Size(max = 20, message = "data deve ter no máximo 20 entradas")
    private Map<
            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,63}$", message = "chave de data inválida") @NotReservedDataKey String,
            @NotNull(message = "valor de data não pode ser nulo") @Size(max = 512, message = "valor de data deve ter no máximo 512 caracteres") String> data;
}
