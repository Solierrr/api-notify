package com.solaria.notify.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UnreadCountResponseDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Quantidade de notificações não lidas", example = "3")
    private long count;
}
