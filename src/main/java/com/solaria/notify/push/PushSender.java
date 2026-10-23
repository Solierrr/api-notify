package com.solaria.notify.push;

import java.util.List;

/**
 * Contrato de envio de push. A Fase 2 implementa com Firebase Cloud Messaging.
 * invalidTokens: tokens que o provedor rejeitou de forma definitiva (o aparelho deve ser removido);
 * nunca nulo (lista vazia quando não há nenhum).
 */
public interface PushSender {

    PushResult send(List<String> fcmTokens, PushMessage message);
}
