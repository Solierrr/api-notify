package com.solaria.notify.openapi;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.solaria.notify.dto.response.NotificationResponseDTO;
import com.solaria.notify.dto.response.PageResponseDTO;
import com.solaria.notify.dto.response.UnreadCountResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;

@Tag(name = "Notificações", description = "Histórico de notificações da pessoa autenticada")
public interface NotificationOpenApi {

    @Operation(
        operationId = "listNotifications",
        summary = "Lista o histórico de notificações",
        description = "Mais recentes primeiro. size acima de 100 é limitado a 100.",
        extensions = @Extension(properties = @ExtensionProperty(name = "x-error-codes", parseValue = true,
                value = "[\"VALIDATION_FAILED\", \"UNAUTHENTICATED\", \"INTERNAL_ERROR\"]"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de notificações"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "422", description = "page menor que 0, size menor que 1 ou parâmetro com formato inválido")
    })
    ResponseEntity<PageResponseDTO<NotificationResponseDTO>> list(
            @Parameter(description = "Número da página, começando em 0", example = "0")
            @Min(value = 0, message = "page deve ser maior ou igual a 0") int page,
            @Parameter(description = "Tamanho da página (máximo efetivo 100)", example = "20")
            @Min(value = 1, message = "size deve ser maior ou igual a 1") int size,
            @Parameter(description = "Somente não lidas", example = "false") boolean unreadOnly);

    @Operation(
        operationId = "countUnreadNotifications",
        summary = "Conta as notificações não lidas",
        description = "Conta só as notificações da pessoa autenticada.",
        extensions = @Extension(properties = @ExtensionProperty(name = "x-error-codes", parseValue = true,
                value = "[\"UNAUTHENTICATED\", \"INTERNAL_ERROR\"]"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantidade de não lidas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<UnreadCountResponseDTO> unreadCount();

    @Operation(
        operationId = "markNotificationRead",
        summary = "Marca uma notificação como lida",
        description = "Idempotente: se já estava lida, mantém o readAt original. "
                + "Notificação inexistente ou de outra pessoa responde 404.",
        extensions = @Extension(properties = @ExtensionProperty(name = "x-error-codes", parseValue = true,
                value = "[\"VALIDATION_FAILED\", \"UNAUTHENTICATED\", \"NOT_FOUND\", \"INTERNAL_ERROR\"]"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificação marcada como lida"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Notificação não encontrada"),
            @ApiResponse(responseCode = "422", description = "id com formato inválido")
    })
    ResponseEntity<NotificationResponseDTO> markRead(
            @Parameter(description = "Id da notificação", example = "3f2b8c1e-5d4a-4e9b-9c7d-1a2b3c4d5e6f") UUID id);

    @Operation(
        operationId = "markAllNotificationsRead",
        summary = "Marca todas as notificações da pessoa como lidas",
        description = "Marca as não lidas da pessoa autenticada; as já lidas mantêm o readAt original.",
        extensions = @Extension(properties = @ExtensionProperty(name = "x-error-codes", parseValue = true,
                value = "[\"UNAUTHENTICATED\", \"INTERNAL_ERROR\"]"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notificações marcadas como lidas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<Void> markAllRead();
}
