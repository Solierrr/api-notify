package com.solaria.notify.dto.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationDispatchResponseDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Id da notificação gravada", example = "3f2b8c1e-5d4a-4e9b-9c7d-1a2b3c4d5e6f")
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Aparelhos que receberam o push", example = "2")
    private int sent;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Aparelhos em que o envio falhou", example = "0")
    private int failed;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Aparelhos removidos por token rejeitado pelo provedor", example = "0")
    private int removedDevices;
}
