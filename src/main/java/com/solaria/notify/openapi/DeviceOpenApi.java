package com.solaria.notify.openapi;

import org.springframework.http.ResponseEntity;

import com.solaria.notify.dto.request.DeviceRegistrationRequestDTO;
import com.solaria.notify.dto.response.DeviceResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@Tag(name = "Aparelhos", description = "Aparelhos da pessoa autenticada que recebem push")
public interface DeviceOpenApi {

    @Operation(
        operationId = "registerDevice",
        summary = "Registra ou atualiza um aparelho",
        description = "Upsert pelo installationId (Firebase Installation ID). Se o aparelho estava "
                + "registrado para outra pessoa (troca de conta no mesmo celular), passa a pertencer "
                + "somente à pessoa autenticada. Navegadores se registram com platform WEB e o token FCM "
                + "do Web Push; o installationId é o Firebase Installation ID do navegador.",
        extensions = @Extension(properties = @ExtensionProperty(name = "x-error-codes", parseValue = true,
                value = "[\"VALIDATION_ERROR\", \"MALFORMED_REQUEST\", \"UNAUTHENTICATED\", \"INTERNAL_ERROR\"]"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aparelho registrado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "400", description = "installationId ou corpo inválido")
    })
    ResponseEntity<DeviceResponseDTO> register(
            @Parameter(description = "Firebase Installation ID (letras, números, _ e -, até 64 caracteres)",
                    example = "fJ3kP9xQ-2mVb7Lw_Zt1Ry")
            @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "installationId inválido") String installationId,
            @Valid DeviceRegistrationRequestDTO dto);

    @Operation(
        operationId = "removeDevice",
        summary = "Remove um aparelho da pessoa autenticada",
        description = "Aparelho inexistente ou de outra pessoa responde 404.",
        extensions = @Extension(properties = @ExtensionProperty(name = "x-error-codes", parseValue = true,
                value = "[\"VALIDATION_ERROR\", \"UNAUTHENTICATED\", \"RESOURCE_NOT_FOUND\", \"INTERNAL_ERROR\"]"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Aparelho removido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Aparelho não encontrado"),
            @ApiResponse(responseCode = "400", description = "installationId inválido")
    })
    ResponseEntity<Void> remove(
            @Parameter(description = "Firebase Installation ID", example = "fJ3kP9xQ-2mVb7Lw_Zt1Ry")
            @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "installationId inválido") String installationId);
}
