package com.solaria.notify.dto.request;

import java.util.Map;
import java.util.UUID;

import com.solaria.notify.model.enums.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
    private Map<String, String> data;
}
