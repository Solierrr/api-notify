package com.solaria.notify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.solaria.notify.dto.request.DeviceRegistrationRequestDTO;
import com.solaria.notify.dto.response.DeviceResponseDTO;
import com.solaria.notify.exception.ResourceNotFoundException;
import com.solaria.notify.model.Device;
import com.solaria.notify.model.enums.Platform;
import com.solaria.notify.repository.DeviceRepository;
import com.solaria.notify.security.CurrentUser;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTests {

    private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");
    private static final Instant EARLIER = Instant.parse("2025-12-01T08:00:00Z");

    @Mock
    DeviceRepository repository;
    @Mock
    CurrentUser currentUser;

    DeviceService service;

    UUID userA = UUID.randomUUID();
    UUID userB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DeviceService(repository, currentUser, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private DeviceRegistrationRequestDTO dto(String token) {
        DeviceRegistrationRequestDTO dto = new DeviceRegistrationRequestDTO();
        dto.setFcmToken(token);
        dto.setPlatform(Platform.ANDROID);
        dto.setAppVersion("1.0.0");
        return dto;
    }

    private Device device(UUID owner) {
        return new Device("fid-1", owner, "old-token", Platform.ANDROID, "0.9", EARLIER, EARLIER);
    }

    @Test
    void registerCreatesDeviceForCurrentUser() {
        when(currentUser.authId()).thenReturn(userA);
        when(repository.findById("fid-1")).thenReturn(Optional.empty());
        when(repository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        DeviceResponseDTO response = service.register("fid-1", dto("tok"));

        ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
        verify(repository).save(captor.capture());
        Device saved = captor.getValue();
        assertThat(saved.getAuthId()).isEqualTo(userA);
        assertThat(saved.getFcmToken()).isEqualTo("tok");
        assertThat(saved.getCreatedAt()).isEqualTo(NOW).isEqualTo(saved.getUpdatedAt());
        assertThat(response.getInstallationId()).isEqualTo("fid-1");
        assertThat(response.getPlatform()).isEqualTo(Platform.ANDROID);
    }

    @Test
    void registerStoresWebPlatformAndBrowserName() {
        when(currentUser.authId()).thenReturn(userA);
        when(repository.findById("fid-web")).thenReturn(Optional.empty());
        when(repository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));
        DeviceRegistrationRequestDTO web = dto("web-token");
        web.setPlatform(Platform.WEB);
        web.setAppVersion("Chrome 130");

        DeviceResponseDTO response = service.register("fid-web", web);

        ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPlatform()).isEqualTo(Platform.WEB);
        assertThat(captor.getValue().getAppVersion()).isEqualTo("Chrome 130");
        assertThat(response.getPlatform()).isEqualTo(Platform.WEB);
    }

    @Test
    void registerUpdatesTokenKeepingCreatedAt() {
        when(currentUser.authId()).thenReturn(userA);
        when(repository.findById("fid-1")).thenReturn(Optional.of(device(userA)));
        when(repository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        service.register("fid-1", dto("new-token"));

        ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getFcmToken()).isEqualTo("new-token");
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(EARLIER);
        assertThat(captor.getValue().getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void registerReassignsDeviceFromAnotherUser() {
        when(currentUser.authId()).thenReturn(userB);
        when(repository.findById("fid-1")).thenReturn(Optional.of(device(userA)));
        when(repository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        service.register("fid-1", dto("tok"));

        ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAuthId()).isEqualTo(userB);
        assertThat(captor.getValue().getInstallationId()).isEqualTo("fid-1");
    }

    @Test
    void removeDeletesOwnDevice() {
        when(currentUser.authId()).thenReturn(userA);
        when(repository.findById("fid-1")).thenReturn(Optional.of(device(userA)));

        service.remove("fid-1");

        verify(repository).deleteById("fid-1");
    }

    @Test
    void removeOtherUsersDeviceThrowsNotFound() {
        when(currentUser.authId()).thenReturn(userB);
        when(repository.findById("fid-1")).thenReturn(Optional.of(device(userA)));

        assertThatThrownBy(() -> service.remove("fid-1")).isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).deleteById(anyString());
    }

    @Test
    void removeUnknownDeviceThrowsNotFound() {
        when(currentUser.authId()).thenReturn(userA);
        when(repository.findById("fid-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove("fid-1")).isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).deleteById(anyString());
    }

    @Test
    void removeStaleDeletesByInstallationIdAndTokenAndCountsActualDeletions() {
        Device current = new Device("fid-1", userA, "t1", Platform.ANDROID, "1.0", NOW, NOW);
        Device reRegistered = new Device("fid-2", userA, "old", Platform.ANDROID, "1.0", NOW, NOW);
        when(repository.deleteByInstallationIdAndFcmToken("fid-1", "t1")).thenReturn(1L);
        when(repository.deleteByInstallationIdAndFcmToken("fid-2", "old")).thenReturn(0L);

        int removed = service.removeStale(java.util.List.of(current, reRegistered));

        assertThat(removed).isEqualTo(1);
        verify(repository).deleteByInstallationIdAndFcmToken("fid-2", "old");
    }
}
