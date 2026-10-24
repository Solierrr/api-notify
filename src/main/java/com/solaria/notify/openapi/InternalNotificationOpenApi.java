package com.solaria.notify.openapi;

import org.springframework.http.ResponseEntity;

import com.solaria.notify.dto.request.InternalNotificationRequestDTO;
import com.solaria.notify.dto.response.NotificationDispatchResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Interno", description = "Disparo de notificações entre serviços (X-Service-Key)")
public interface InternalNotificationOpenApi {

    @Operation(
        summary = "Grava e dispara uma notificação push",
        description = "Sempre grava no histórico; se a pessoa não tiver aparelhos, responde 201 com sent=0. "
                + "Aparelhos com token rejeitado pelo provedor são removidos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notificação gravada e disparada"),
            @ApiResponse(responseCode = "400", description = "Corpo inválido"),
            @ApiResponse(responseCode = "401", description = "Chave de serviço ausente ou inválida")
    })
    ResponseEntity<NotificationDispatchResponseDTO> dispatch(@Valid InternalNotificationRequestDTO request);
}
