package com.solaria.notify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.solaria.notify.dto.request.InternalNotificationRequestDTO;
import com.solaria.notify.dto.response.NotificationDispatchResponseDTO;
import com.solaria.notify.model.Device;
import com.solaria.notify.model.Notification;
import com.solaria.notify.model.enums.NotificationType;
import com.solaria.notify.model.enums.Platform;
import com.solaria.notify.push.PushMessage;
import com.solaria.notify.push.PushResult;
import com.solaria.notify.push.PushSender;
import com.solaria.notify.repository.NotificationRepository;
import com.solaria.notify.security.CurrentUser;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchTests {

    private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");

    @Mock
    NotificationRepository repository;
    @Mock
    MongoTemplate mongoTemplate;
    @Mock
    CurrentUser currentUser;
    @Mock
    DeviceService deviceService;
    @Mock
    PushSender pushSender;

    NotificationService service;

    UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, mongoTemplate, currentUser, Clock.fixed(NOW, ZoneOffset.UTC),
                deviceService, pushSender);
        when(currentUser.serviceName()).thenReturn("api-core");
        when(repository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
    }

    private InternalNotificationRequestDTO request() {
        InternalNotificationRequestDTO dto = new InternalNotificationRequestDTO();
        dto.setAuthId(user);
        dto.setType(NotificationType.NEW_MESSAGE);
        dto.setTitle("Titulo");
        dto.setBody("Corpo");
        dto.setData(Map.of("k", "v"));
        return dto;
    }

    private static Device device(String fid, String token) {
        return new Device(fid, UUID.randomUUID(), token, Platform.ANDROID, "1.0", NOW, NOW);
    }

    private void twoDevices() {
        when(deviceService.findByAuthId(user)).thenReturn(List.of(device("fid-1", "t1"), device("fid-2", "t2")));
    }

    @Test
    void dispatchSavesWithSourceServiceAndExpiry() {
        when(deviceService.findByAuthId(user)).thenReturn(List.of());

        NotificationDispatchResponseDTO result = service.dispatch(request());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getSourceService()).isEqualTo("api-core");
        assertThat(saved.getAuthId()).isEqualTo(user);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getExpireAt()).isEqualTo(saved.getCreatedAt().plus(90, ChronoUnit.DAYS));
        assertThat(saved.getReadAt()).isNull();
        assertThat(result.getId()).isEqualTo(saved.getId());
    }

    @Test
    void dispatchWithoutDevicesStillSaves() {
        when(deviceService.findByAuthId(user)).thenReturn(List.of());

        NotificationDispatchResponseDTO result = service.dispatch(request());

        verify(repository).save(any(Notification.class));
        verify(pushSender, never()).send(any(), any());
        assertThat(result.getSent()).isZero();
        assertThat(result.getFailed()).isZero();
        assertThat(result.getRemovedDevices()).isZero();
    }

    @Test
    void dispatchSendsToAllUserTokens() {
        twoDevices();
        when(pushSender.send(any(), any())).thenReturn(new PushResult(2, 0, List.of()));

        NotificationDispatchResponseDTO result = service.dispatch(request());

        ArgumentCaptor<PushMessage> msg = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushSender).send(eq(List.of("t1", "t2")), msg.capture());
        assertThat(msg.getValue().notificationId()).isEqualTo(result.getId());
        assertThat(msg.getValue().type()).isEqualTo(NotificationType.NEW_MESSAGE);
        assertThat(result.getSent()).isEqualTo(2);
        verify(deviceService, never()).removeStale(any());
    }

    @Test
    void dispatchRemovesDevicesWithInvalidTokens() {
        Device d1 = device("fid-1", "t1");
        Device d2 = device("fid-2", "t2");
        when(deviceService.findByAuthId(user)).thenReturn(List.of(d1, d2));
        when(pushSender.send(any(), any())).thenReturn(new PushResult(1, 1, List.of("t2")));
        when(deviceService.removeStale(any())).thenReturn(1);

        NotificationDispatchResponseDTO result = service.dispatch(request());

        verify(deviceService).removeStale(List.of(d2));
        assertThat(result.getRemovedDevices()).isEqualTo(1);
        assertThat(result.getSent()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(1);
    }

    @Test
    void removedDevicesReflectsActualDeletions() {
        twoDevices();
        when(pushSender.send(any(), any())).thenReturn(new PushResult(1, 1, List.of("t2")));
        // aparelho foi refeito com outro token durante o envio: nada removido
        when(deviceService.removeStale(any())).thenReturn(0);

        NotificationDispatchResponseDTO result = service.dispatch(request());

        assertThat(result.getRemovedDevices()).isZero();
    }

    @Test
    void sharedInvalidTokenRemovesAllMatchingDevices() {
        Device d1 = device("fid-1", "shared");
        Device d2 = device("fid-2", "shared");
        when(deviceService.findByAuthId(user)).thenReturn(List.of(d1, d2));
        when(pushSender.send(any(), any())).thenReturn(new PushResult(0, 2, List.of("shared")));
        when(deviceService.removeStale(any())).thenReturn(2);

        NotificationDispatchResponseDTO result = service.dispatch(request());

        verify(deviceService).removeStale(List.of(d1, d2));
        assertThat(result.getRemovedDevices()).isEqualTo(2);
    }

    @Test
    void nullInvalidTokensIsSafe() {
        twoDevices();
        when(pushSender.send(any(), any())).thenReturn(new PushResult(2, 0, null));

        NotificationDispatchResponseDTO result = service.dispatch(request());

        verify(deviceService, never()).removeStale(any());
        assertThat(result.getSent()).isEqualTo(2);
        assertThat(result.getRemovedDevices()).isZero();
    }

    @Test
    void cleanupFailureDoesNotBreakDispatchResponse() {
        twoDevices();
        when(pushSender.send(any(), any())).thenReturn(new PushResult(1, 1, List.of("t2")));
        when(deviceService.removeStale(any())).thenThrow(new RuntimeException("mongo down"));

        NotificationDispatchResponseDTO result = service.dispatch(request());

        assertThat(result.getSent()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getRemovedDevices()).isZero();
    }

    @Test
    void senderExceptionKeepsNotification() {
        twoDevices();
        when(pushSender.send(any(), any())).thenThrow(new RuntimeException("fcm down"));

        NotificationDispatchResponseDTO result = service.dispatch(request());

        verify(repository).save(any(Notification.class));
        assertThat(result.getFailed()).isEqualTo(2);
        assertThat(result.getSent()).isZero();
        assertThat(result.getRemovedDevices()).isZero();
    }
}
