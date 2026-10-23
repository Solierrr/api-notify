package com.solaria.notify.openapi;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.solaria.notify.dto.response.NotificationResponseDTO;
import com.solaria.notify.dto.response.PageResponseDTO;
import com.solaria.notify.dto.response.UnreadCountResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;

@Tag(name = "Notificações", description = "Histórico de notificações da pessoa autenticada")
public interface NotificationOpenApi {

    @Operation(
        summary = "Lista o histórico de notificações",
        description = "Mais recentes primeiro. size acima de 100 é limitado a 100."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de notificações"),
            @ApiResponse(responseCode = "400", description = "page menor que 0 ou size menor que 1"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<PageResponseDTO<NotificationResponseDTO>> list(
            @Parameter(description = "Número da página, começando em 0")
            @Min(value = 0, message = "page deve ser maior ou igual a 0") int page,
            @Parameter(description = "Tamanho da página (máximo efetivo 100)")
            @Min(value = 1, message = "size deve ser maior ou igual a 1") int size,
            @Parameter(description = "Somente não lidas") boolean unreadOnly);

    @Operation(summary = "Conta as notificações não lidas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantidade de não lidas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<UnreadCountResponseDTO> unreadCount();

    @Operation(
        summary = "Marca uma notificação como lida",
        description = "Idempotente: se já estava lida, mantém o readAt original. "
                + "Notificação inexistente ou de outra pessoa responde 404."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificação marcada como lida"),
            @ApiResponse(responseCode = "400", description = "id inválido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Notificação não encontrada")
    })
    ResponseEntity<NotificationResponseDTO> markRead(
            @Parameter(description = "Id da notificação") UUID id);

    @Operation(summary = "Marca todas as notificações da pessoa como lidas")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notificações marcadas como lidas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<Void> markAllRead();
}
