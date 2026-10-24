package com.solaria.notify.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.solaria.notify.dto.request.DeviceRegistrationRequestDTO;
import com.solaria.notify.dto.response.DeviceResponseDTO;
import com.solaria.notify.openapi.DeviceOpenApi;
import com.solaria.notify.service.DeviceService;


@RestController
@RequestMapping("/notify/devices")
public class DeviceController implements DeviceOpenApi {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    @PutMapping("/{installationId}")
    public ResponseEntity<DeviceResponseDTO> register(
            @PathVariable String installationId,
            @RequestBody DeviceRegistrationRequestDTO dto) {
        return ResponseEntity.ok(deviceService.register(installationId, dto));
    }

    @Override
    @DeleteMapping("/{installationId}")
    public ResponseEntity<Void> remove(
            @PathVariable String installationId) {
        deviceService.remove(installationId);
        return ResponseEntity.noContent().build();
    }
}
