package com.solaria.notify.push;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Implementação padrão enquanto o FCM não está habilitado: só registra o disparo, nunca o token
@Component
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public PushResult send(List<String> fcmTokens, PushMessage message) {
        log.info("Push simulado: notificationId={} type={} tokens={}", message.notificationId(), message.type(),
                fcmTokens.size());
        return new PushResult(fcmTokens.size(), 0, List.of());
    }
}
