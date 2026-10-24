package com.solaria.notify.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Tipo da notificação")
public enum NotificationType {
    NEW_MESSAGE,
    SERVICE_SCHEDULED,
    SERVICE_STATUS_CHANGED
}
