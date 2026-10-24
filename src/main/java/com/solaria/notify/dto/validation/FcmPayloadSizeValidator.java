package com.solaria.notify.dto.validation;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.solaria.notify.dto.request.InternalNotificationRequestDTO;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FcmPayloadSizeValidator implements ConstraintValidator<FcmPayloadSize, InternalNotificationRequestDTO> {

    // margem sob o limite de 4096 bytes do FCM
    static final int MAX_PAYLOAD_BYTES = 3800;

    // notificationId é um UUID gerado no servidor; em string ocupa 36 bytes
    private static final int UUID_STRING_BYTES = 36;

    private static final String[] FIXED_KEYS = { "notificationId", "type", "title", "body" };

    @Override
    public boolean isValid(InternalNotificationRequestDTO dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }
        long total = UUID_STRING_BYTES;
        for (String key : FIXED_KEYS) {
            total += bytes(key);
        }
        total += bytes(dto.getType() == null ? null : dto.getType().name());
        total += bytes(dto.getTitle());
        total += bytes(dto.getBody());
        Map<String, String> data = dto.getData();
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                total += bytes(entry.getKey()) + bytes(entry.getValue());
            }
        }
        return total <= MAX_PAYLOAD_BYTES;
    }

    private static int bytes(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }
}
