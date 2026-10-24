package com.solaria.notify.openapi;

import org.springframework.http.ResponseEntity;

import com.solaria.notify.dto.request.InternalNotificationRequestDTO;
import com.solaria.notify.dto.response.NotificationDispatchResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Interno", description = "Disparo de notificações entre serviços (X-Service-Key)")
@SecurityRequirement(name = "serviceKey")
public interface InternalNotificationOpenApi {

    @Operation(
        operationId = "dispatchNotification",
        summary = "Grava e dispara uma notificação push",
        description = "Sempre grava no histórico; se a pessoa não tiver aparelhos, responde 201 com sent=0. "
                + "Aparelhos com token rejeitado pelo provedor são removidos. A soma dos bytes UTF-8 de "
                + "title, body, type e data não pode passar de 3800 bytes (limite do FCM com margem). "
                + "Chamada pelos serviços com a chave de serviço própria; nunca exposta pelo gateway.",
        extensions = @Extension(properties = @ExtensionProperty(name = "x-error-codes", parseValue = true,
                value = "[\"VALIDATION_FAILED\", \"MALFORMED_REQUEST\", \"UNAUTHENTICATED\", \"INTERNAL_ERROR\"]"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notificação gravada e disparada"),
            @ApiResponse(responseCode = "401", description = "Chave de serviço ausente ou inválida"),
            @ApiResponse(responseCode = "422", description = "Corpo inválido")
    })
    ResponseEntity<NotificationDispatchResponseDTO> dispatch(@Valid InternalNotificationRequestDTO request);
}
