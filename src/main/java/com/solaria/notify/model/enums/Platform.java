package com.solaria.notify.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Plataforma do aparelho: ANDROID (app mobile) ou WEB (navegador, token FCM do Web Push)")
public enum Platform {
    ANDROID,
    WEB
}
