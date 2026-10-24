package com.solaria.notify.dto.request;

import com.solaria.notify.model.enums.Platform;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceRegistrationRequestDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
    @NotBlank(message = "fcmToken é obrigatório")
    @Size(min = 1, max = 4096, message = "fcmToken deve ter entre 1 e 4096 caracteres")
    private String fcmToken;

    @NotNull(message = "platform é obrigatório")
    private Platform platform;

    @Size(max = 32, message = "appVersion deve ter no máximo 32 caracteres")
    private String appVersion;
}
