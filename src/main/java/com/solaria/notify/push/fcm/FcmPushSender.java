package com.solaria.notify.push.fcm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.solaria.notify.push.PushDeliveryException;
import com.solaria.notify.push.PushMessage;
import com.solaria.notify.push.PushResult;
import com.solaria.notify.push.PushSender;

// Envia mensagens só de dados (sem Notification) com prioridade alta; nunca loga token
@Component
@ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "true")
public class FcmPushSender implements PushSender {

    static final int MAX_TOKENS_PER_BATCH = 500;

    // Respostas que indicam token morto: o aparelho deve ser removido
    private static final Set<MessagingErrorCode> INVALID_TOKEN_CODES = Set.of(MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT, MessagingErrorCode.SENDER_ID_MISMATCH);

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);

    private final FirebaseMessaging firebaseMessaging;
    private final FcmProperties properties;

    public FcmPushSender(FirebaseMessaging firebaseMessaging, FcmProperties properties) {
        this.firebaseMessaging = firebaseMessaging;
        this.properties = properties;
    }

    @Override
    public PushResult send(List<String> fcmTokens, PushMessage message) {
        Map<String, String> payload = payload(message);
        int sent = 0;
        int failed = 0;
        int failedBatches = 0;
        int batchCount = 0;
        int totalResponses = 0;
        int senderMismatches = 0;
        Map<String, Integer> codes = new TreeMap<>();
        List<String> invalid = new ArrayList<>();

        PushDeliveryException lastFailure = null;
        for (int from = 0; from < fcmTokens.size(); from += MAX_TOKENS_PER_BATCH) {
            int batchIndex = batchCount++;
            List<String> batch = fcmTokens.subList(from, Math.min(from + MAX_TOKENS_PER_BATCH, fcmTokens.size()));
            MulticastMessage multicast = MulticastMessage.builder()
                    .addAllTokens(batch)
                    .putAllData(payload)
                    .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                    .build();
            BatchResponse response;
            try {
                response = firebaseMessaging.sendEachForMulticast(multicast, properties.isDryRun());
            } catch (FirebaseMessagingException e) {
                // Sem causa encadeada: só os códigos, para a mensagem nunca carregar token
                String detail = "errorCode=" + e.getErrorCode() + " messagingErrorCode=" + e.getMessagingErrorCode();
                log.warn("Lote FCM falhou: notificationId={} batch={} {}", message.notificationId(), batchIndex,
                        detail);
                lastFailure = new PushDeliveryException("Falha ao enviar lote FCM: " + detail);
                failedBatches++;
                failed += batch.size();
                continue;
            }
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse r = responses.get(i);
                totalResponses++;
                if (r.isSuccessful()) {
                    sent++;
                    continue;
                }
                failed++;
                FirebaseMessagingException error = r.getException();
                codes.merge(codeName(error), 1, Integer::sum);
                if (error != null && error.getMessagingErrorCode() != null
                        && INVALID_TOKEN_CODES.contains(error.getMessagingErrorCode())) {
                    invalid.add(batch.get(i));
                    if (error.getMessagingErrorCode() == MessagingErrorCode.SENDER_ID_MISMATCH) {
                        senderMismatches++;
                    }
                }
            }
        }

        if (batchCount > 0 && failedBatches == batchCount) {
            throw lastFailure;
        }

        if (totalResponses > 0 && senderMismatches == totalResponses) {
            // Todos os tokens recusados por SENDER_ID_MISMATCH: quase certo que a credencial é de outro
            // projeto Firebase; remover os aparelhos apagaria tokens que estão corretos
            log.error("FCM recusou todos os tokens com SENDER_ID_MISMATCH: a credencial provavelmente pertence "
                    + "a outro projeto Firebase. Aparelhos mantidos. notificationId={} failed={}",
                    message.notificationId(), failed);
            invalid.clear();
        }

        if (failed > 0) {
            log.warn("Push FCM com falhas: notificationId={} type={} sent={} failed={} invalid={} dryRun={} codes={}",
                    message.notificationId(), message.type(), sent, failed, invalid.size(), properties.isDryRun(),
                    codes);
        } else {
            log.info("Push FCM: notificationId={} type={} sent={} failed={} invalid={} dryRun={}",
                    message.notificationId(), message.type(), sent, failed, invalid.size(), properties.isDryRun());
        }
        return new PushResult(sent, failed, invalid);
    }

    // Nome do código de erro por token (nunca o token): MessagingErrorCode, senão o código de plataforma
    private static String codeName(FirebaseMessagingException error) {
        if (error == null) {
            return "NO_EXCEPTION";
        }
        if (error.getMessagingErrorCode() != null) {
            return error.getMessagingErrorCode().name();
        }
        return error.getErrorCode() != null ? error.getErrorCode().name() : "NO_EXCEPTION";
    }

    private static Map<String, String> payload(PushMessage message) {
        Map<String, String> payload = new HashMap<>();
        if (message.data() != null) {
            payload.putAll(message.data());
        }
        // Chaves fixas por último: nunca sobrescritas pelo data da requisição
        payload.put("notificationId", String.valueOf(message.notificationId()));
        payload.put("type", message.type().name());
        payload.put("title", message.title());
        payload.put("body", message.body());
        return payload;
    }
}
