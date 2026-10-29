package com.solaria.notify.dto.response;

import java.time.Instant;

import com.solaria.notify.model.enums.Platform;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceResponseDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Firebase Installation ID", example = "fJ3kP9xQ-2mVb7Lw_Zt1Ry")
    private String installationId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Plataforma do aparelho")
    private Platform platform;
    @Schema(description = "Versão do app instalada no aparelho; em aparelhos WEB, o navegador (ex.: Chrome 130)", example = "1.4.0")
    private String appVersion;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Última atualização do registro", example = "2026-10-24T13:45:00Z")
    private Instant updatedAt;
}
