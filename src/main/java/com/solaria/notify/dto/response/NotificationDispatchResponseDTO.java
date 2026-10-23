package com.solaria.notify.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationDispatchResponseDTO {

    private UUID id;
    private int sent;
    private int failed;
    private int removedDevices;
}
