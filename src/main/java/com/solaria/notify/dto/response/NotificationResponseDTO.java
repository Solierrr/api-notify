package com.solaria.notify.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.solaria.notify.model.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationResponseDTO {

    private UUID id;
    private NotificationType type;
    private String title;
    private String body;
    private Map<String, String> data;
    private Instant createdAt;
    private Instant readAt;
}
