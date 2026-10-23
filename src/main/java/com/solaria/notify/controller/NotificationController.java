package com.solaria.notify.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.solaria.notify.dto.response.NotificationResponseDTO;
import com.solaria.notify.dto.response.PageResponseDTO;
import com.solaria.notify.dto.response.UnreadCountResponseDTO;
import com.solaria.notify.openapi.NotificationOpenApi;
import com.solaria.notify.service.NotificationService;

@RestController
@RequestMapping("/notify/notifications")
public class NotificationController implements NotificationOpenApi {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    @GetMapping
    public ResponseEntity<PageResponseDTO<NotificationResponseDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return ResponseEntity.ok(notificationService.list(page, size, unreadOnly));
    }

    @Override
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponseDTO> unreadCount() {
        return ResponseEntity.ok(notificationService.unreadCount());
    }

    @Override
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markRead(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.markRead(id));
    }

    @Override
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.noContent().build();
    }
}
