package com.solaria.notify.dto.response;

import java.time.Instant;

import com.solaria.notify.model.enums.Platform;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceResponseDTO {

    private String installationId;
    private Platform platform;
    private String appVersion;
    private Instant updatedAt;
}
