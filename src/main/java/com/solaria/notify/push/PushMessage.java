package com.solaria.notify.push;

import java.util.Map;
import java.util.UUID;

import com.solaria.notify.model.enums.NotificationType;

public record PushMessage(UUID notificationId, NotificationType type, String title, String body,
        Map<String, String> data) {
}
