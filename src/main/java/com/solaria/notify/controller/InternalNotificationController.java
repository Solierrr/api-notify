package com.solaria.notify.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.solaria.notify.dto.request.InternalNotificationRequestDTO;
import com.solaria.notify.dto.response.NotificationDispatchResponseDTO;
import com.solaria.notify.openapi.InternalNotificationOpenApi;
import com.solaria.notify.service.NotificationService;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController implements InternalNotificationOpenApi {

    private final NotificationService notificationService;

    public InternalNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    @PostMapping
    public ResponseEntity<NotificationDispatchResponseDTO> dispatch(@RequestBody InternalNotificationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.dispatch(request));
    }
}
