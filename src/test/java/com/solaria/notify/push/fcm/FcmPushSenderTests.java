package com.solaria.notify.push.fcm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.solaria.notify.model.enums.NotificationType;
import com.solaria.notify.push.PushDeliveryException;
import com.solaria.notify.push.PushMessage;
import com.solaria.notify.push.PushResult;

class FcmPushSenderTests {

    FirebaseMessaging firebase = mock(FirebaseMessaging.class);
    FcmProperties properties = new FcmProperties();
    FcmPushSender sender;
    UUID id = UUID.randomUUID();

    ListAppender<ILoggingEvent> logs = new ListAppender<>();
    Logger senderLogger = (Logger) LoggerFactory.getLogger(FcmPushSender.class);

    @BeforeEach
    void setUp() {
        sender = new FcmPushSender(firebase, properties);
        logs.start();
        senderLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        senderLogger.detachAppender(logs);
    }

    private static SendResponse errorWithPlatformCode(ErrorCode code) {
        SendResponse r = mock(SendResponse.class);
        FirebaseMessagingException e = mock(FirebaseMessagingException.class);
        when(e.getErrorCode()).thenReturn(code);
        when(r.isSuccessful()).thenReturn(false);
        when(r.getException()).thenReturn(e);
        return r;
    }

    private static SendResponse errorWithoutException() {
        SendResponse r = mock(SendResponse.class);
        when(r.isSuccessful()).thenReturn(false);
        return r;
    }

    private PushMessage message(Map<String, String> data) {
        return new PushMessage(id, NotificationType.NEW_MESSAGE, "Titulo", "Corpo", data);
    }

    private static SendResponse ok() {
        SendResponse r = mock(SendResponse.class);
        when(r.isSuccessful()).thenReturn(true);
        return r;
    }

    private static SendResponse error(MessagingErrorCode code) {
        SendResponse r = mock(SendResponse.class);
        FirebaseMessagingException e = mock(FirebaseMessagingException.class);
        when(e.getMessagingErrorCode()).thenReturn(code);
        when(r.isSuccessful()).thenReturn(false);
        when(r.getException()).thenReturn(e);
        return r;
    }

    private static BatchResponse batch(List<SendResponse> responses) {
        BatchResponse b = mock(BatchResponse.class);
        when(b.getResponses()).thenReturn(responses);
        return b;
    }

    private void respond(SendResponse... responses) throws Exception {
        BatchResponse b = batch(List.of(responses));
        when(firebase.sendEachForMulticast(any(), eq(false))).thenReturn(b);
    }

    private static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsDataOnlyHighPriorityMessage() throws Exception {
        respond(ok());

        sender.send(List.of("t1"), message(Map.of("chatId", "42")));

        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebase).sendEachForMulticast(captor.capture(), eq(false));
        MulticastMessage sent = captor.getValue();
        // leitura por reflexão: o SDK 9.10.0 (fixo) não expõe getters
        assertThat((Map<String, String>) field(sent, "data")).containsOnly(
                Map.entry("notificationId", id.toString()), Map.entry("type", "NEW_MESSAGE"),
                Map.entry("title", "Titulo"), Map.entry("body", "Corpo"), Map.entry("chatId", "42"));
        assertThat(field(sent, "notification")).isNull();
        assertThat((List<String>) field(sent, "tokens")).containsExactly("t1");
        Object android = field(sent, "androidConfig");
        assertThat(android).isNotNull();
        assertThat(field(android, "priority")).isEqualTo("high");
        assertThat(field(android, "notification")).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsWebPushWithHighUrgency() throws Exception {
        respond(ok());

        sender.send(List.of("t1"), message(Map.of()));

        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebase).sendEachForMulticast(captor.capture(), eq(false));
        Object webpush = field(captor.getValue(), "webpushConfig");
        assertThat(webpush).isNotNull();
        assertThat((Map<String, String>) field(webpush, "headers")).containsEntry("Urgency", "high");
        assertThat(field(webpush, "notification")).isNull();
    }

    @Test
    void allSuccessReturnsSentCount() throws Exception {
        respond(ok(), ok(), ok());

        PushResult result = sender.send(List.of("t1", "t2", "t3"), message(null));

        assertThat(result).isEqualTo(new PushResult(3, 0, List.of()));
    }

    @Test
    void unregisteredInvalidArgumentAndSenderMismatchAreInvalid() throws Exception {
        respond(ok(), error(MessagingErrorCode.UNREGISTERED), error(MessagingErrorCode.INVALID_ARGUMENT),
                error(MessagingErrorCode.SENDER_ID_MISMATCH));

        PushResult result = sender.send(List.of("t1", "t2", "t3", "t4"), message(null));

        assertThat(result).isEqualTo(new PushResult(1, 3, List.of("t2", "t3", "t4")));
    }

    @Test
    void transientErrorsCountAsFailedButKeepDevice() throws Exception {
        respond(error(MessagingErrorCode.UNAVAILABLE), error(MessagingErrorCode.QUOTA_EXCEEDED),
                error(MessagingErrorCode.INTERNAL));

        PushResult result = sender.send(List.of("t1", "t2", "t3"), message(null));

        assertThat(result.sent()).isZero();
        assertThat(result.failed()).isEqualTo(3);
        assertThat(result.invalidTokens()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void splitsIntoBatchesOf500() throws Exception {
        List<String> tokens = IntStream.range(0, 1201).mapToObj(i -> "tok-" + i).toList();
        when(firebase.sendEachForMulticast(any(), eq(false))).thenAnswer(inv -> {
            MulticastMessage m = inv.getArgument(0);
            int size = ((List<String>) field(m, "tokens")).size();
            List<SendResponse> rs = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                rs.add(i == 0 ? error(MessagingErrorCode.UNREGISTERED) : ok());
            }
            return batch(rs);
        });

        PushResult result = sender.send(tokens, message(null));

        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebase, times(3)).sendEachForMulticast(captor.capture(), eq(false));
        List<List<String>> batches = new ArrayList<>();
        for (MulticastMessage m : captor.getAllValues()) {
            batches.add((List<String>) field(m, "tokens"));
        }
        assertThat(batches).extracting(List::size).containsExactly(500, 500, 201);
        assertThat(result.sent()).isEqualTo(1198);
        assertThat(result.failed()).isEqualTo(3);
        // índice 0 de cada lote mapeia para o token certo
        assertThat(result.invalidTokens()).containsExactly("tok-0", "tok-500", "tok-1000");
    }

    @Test
    void wholeBatchFailureThrowsPushDeliveryException() throws Exception {
        FirebaseMessagingException e = mock(FirebaseMessagingException.class);
        when(e.getErrorCode()).thenReturn(ErrorCode.UNAUTHENTICATED);
        when(e.getMessagingErrorCode()).thenReturn(MessagingErrorCode.THIRD_PARTY_AUTH_ERROR);
        when(firebase.sendEachForMulticast(any(), eq(false))).thenThrow(e);

        assertThatThrownBy(() -> sender.send(List.of("secret-token-1", "secret-token-2"), message(null)))
                .isInstanceOf(PushDeliveryException.class)
                .hasMessageContaining("UNAUTHENTICATED")
                .hasMessageContaining("THIRD_PARTY_AUTH_ERROR")
                .hasMessageNotContaining("secret-token")
                .hasNoCause();
    }

    @Test
    void failuresAreLoggedAtWarnWithCodeSummaryAndNoTokens() throws Exception {
        respond(ok(), error(MessagingErrorCode.UNAVAILABLE), error(MessagingErrorCode.UNAVAILABLE),
                errorWithPlatformCode(ErrorCode.PERMISSION_DENIED), errorWithoutException());

        sender.send(List.of("secret-1", "secret-2", "secret-3", "secret-4", "secret-5"), message(null));

        List<ILoggingEvent> warns = logs.list.stream().filter(e -> e.getLevel() == Level.WARN).toList();
        assertThat(warns).hasSize(1);
        String line = warns.get(0).getFormattedMessage();
        assertThat(line).contains("sent=1", "failed=4", "invalid=0", "dryRun=false",
                "codes={NO_EXCEPTION=1, PERMISSION_DENIED=1, UNAVAILABLE=2}").doesNotContain("secret");
    }

    @Test
    void fullSuccessLogsAtInfoOnly() throws Exception {
        respond(ok(), ok());

        sender.send(List.of("t1", "t2"), message(null));

        assertThat(logs.list).noneMatch(e -> e.getLevel() == Level.WARN || e.getLevel() == Level.ERROR);
        assertThat(logs.list).anyMatch(e -> e.getLevel() == Level.INFO);
    }

    @Test
    void allSenderIdMismatchKeepsDevicesAndLogsError() throws Exception {
        respond(error(MessagingErrorCode.SENDER_ID_MISMATCH), error(MessagingErrorCode.SENDER_ID_MISMATCH));

        PushResult result = sender.send(List.of("secret-1", "secret-2"), message(null));

        assertThat(result.sent()).isZero();
        assertThat(result.failed()).isEqualTo(2);
        assertThat(result.invalidTokens()).isEmpty();
        List<ILoggingEvent> errors = logs.list.stream().filter(e -> e.getLevel() == Level.ERROR).toList();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getFormattedMessage()).contains("SENDER_ID_MISMATCH").doesNotContain("secret");
    }

    @Test
    void senderIdMismatchMixedWithSuccessStillRemovesDevice() throws Exception {
        respond(ok(), error(MessagingErrorCode.SENDER_ID_MISMATCH));

        PushResult result = sender.send(List.of("t1", "t2"), message(null));

        assertThat(result).isEqualTo(new PushResult(1, 1, List.of("t2")));
        assertThat(logs.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }

    @Test
    void dryRunFlagIsPassedToFirebase() throws Exception {
        properties.setDryRun(true);
        BatchResponse b = batch(List.of(ok()));
        when(firebase.sendEachForMulticast(any(), eq(true))).thenReturn(b);

        PushResult result = sender.send(List.of("t1"), message(null));

        verify(firebase).sendEachForMulticast(any(), eq(true));
        assertThat(result.sent()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void nullDataIsSafe() throws Exception {
        respond(ok());

        sender.send(List.of("t1"), message(null));

        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebase).sendEachForMulticast(captor.capture(), eq(false));
        assertThat((Map<String, String>) field(captor.getValue(), "data"))
                .containsOnlyKeys("notificationId", "type", "title", "body");
    }

    @Test
    void nullMessagingErrorCodeCountsAsFailedAndKeepsDevice() throws Exception {
        respond(error(null), ok());

        PushResult result = sender.send(List.of("t1", "t2"), message(null));

        assertThat(result).isEqualTo(new PushResult(1, 1, List.of()));
    }

    @Test
    void failedResponseWithoutExceptionCountsAsFailed() throws Exception {
        SendResponse r = mock(SendResponse.class);
        when(r.isSuccessful()).thenReturn(false);
        when(r.getException()).thenReturn(null);
        respond(r);

        PushResult result = sender.send(List.of("t1"), message(null));

        assertThat(result).isEqualTo(new PushResult(0, 1, List.of()));
    }

    private static FirebaseMessagingException batchError() {
        FirebaseMessagingException e = mock(FirebaseMessagingException.class);
        when(e.getErrorCode()).thenReturn(ErrorCode.UNAVAILABLE);
        when(e.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
        return e;
    }

    @Test
    @SuppressWarnings("unchecked")
    void laterBatchFailureKeepsEarlierResults() throws Exception {
        List<String> tokens = IntStream.range(0, 1201).mapToObj(i -> "tok-" + i).toList();
        int[] call = {0};
        when(firebase.sendEachForMulticast(any(), eq(false))).thenAnswer(inv -> {
            if (++call[0] == 3) {
                throw batchError();
            }
            MulticastMessage m = inv.getArgument(0);
            int size = ((List<String>) field(m, "tokens")).size();
            List<SendResponse> rs = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                rs.add(i == 0 ? error(MessagingErrorCode.UNREGISTERED) : ok());
            }
            return batch(rs);
        });

        PushResult result = sender.send(tokens, message(null));

        assertThat(result.sent()).isEqualTo(998);
        assertThat(result.failed()).isEqualTo(203);
        assertThat(result.invalidTokens()).containsExactly("tok-0", "tok-500");
    }

    @Test
    void allBatchesFailingThrows() throws Exception {
        List<String> tokens = IntStream.range(0, 600).mapToObj(i -> "secret-" + i).toList();
        FirebaseMessagingException failure = batchError();
        when(firebase.sendEachForMulticast(any(), eq(false))).thenThrow(failure);

        assertThatThrownBy(() -> sender.send(tokens, message(null)))
                .isInstanceOf(PushDeliveryException.class)
                .hasMessageNotContaining("secret-");
        verify(firebase, times(2)).sendEachForMulticast(any(), eq(false));
    }
}
