package com.solaria.notify.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.solaria.notify.dto.request.DeviceRegistrationRequestDTO;
import com.solaria.notify.dto.response.DeviceResponseDTO;
import com.solaria.notify.exception.ResourceNotFoundException;
import com.solaria.notify.model.Device;
import com.solaria.notify.repository.DeviceRepository;
import com.solaria.notify.security.CurrentUser;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final CurrentUser currentUser;
    private final Clock clock;

    public DeviceService(DeviceRepository deviceRepository, CurrentUser currentUser, Clock clock) {
        this.deviceRepository = deviceRepository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    // Upsert por installationId; se o aparelho era de outra pessoa (troca de conta), passa a ser só do usuário atual
    public DeviceResponseDTO register(String installationId, DeviceRegistrationRequestDTO dto) {
        UUID authId = currentUser.authId();
        Instant now = Instant.now(clock);

        Device device = deviceRepository.findById(installationId).orElseGet(() -> {
            Device created = new Device();
            created.setInstallationId(installationId);
            created.setCreatedAt(now);
            return created;
        });
        device.setAuthId(authId);
        device.setFcmToken(dto.getFcmToken());
        device.setPlatform(dto.getPlatform());
        device.setAppVersion(dto.getAppVersion());
        device.setUpdatedAt(now);

        Device saved = deviceRepository.save(device);
        return new DeviceResponseDTO(saved.getInstallationId(), saved.getPlatform(), saved.getAppVersion(),
                saved.getUpdatedAt());
    }

    // Aparelho de outra pessoa responde 404 igual a inexistente, para não revelar que existe
    public void remove(String installationId) {
        UUID authId = currentUser.authId();
        Device device = deviceRepository.findById(installationId)
                .filter(d -> authId.equals(d.getAuthId()))
                .orElseThrow(() -> new ResourceNotFoundException("Aparelho não encontrado"));
        deviceRepository.deleteById(device.getInstallationId());
    }

    public List<Device> findByAuthId(UUID authId) {
        return deviceRepository.findByAuthId(authId);
    }

    // Limpeza de tokens inválidos reportados pelo FCM: só remove se o aparelho ainda tem o mesmo token,
    // para não apagar um registro refeito (novo token ou troca de conta) durante o envio
    public int removeStale(Collection<Device> devices) {
        int removed = 0;
        for (Device device : devices) {
            removed += (int) deviceRepository.deleteByInstallationIdAndFcmToken(device.getInstallationId(),
                    device.getFcmToken());
        }
        return removed;
    }
}
