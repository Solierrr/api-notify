package com.solaria.notify.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.solaria.notify.dto.request.InternalNotificationRequestDTO;
import com.solaria.notify.model.enums.NotificationType;

class FcmPayloadSizeValidatorTests {

    private final FcmPayloadSizeValidator validator = new FcmPayloadSizeValidator();

    // chaves fixas (14+4+5+4) + UUID (36) + type.name()
    private static final int FIXED = "notificationId".length() + "type".length() + "title".length()
            + "body".length() + 36 + NotificationType.NEW_MESSAGE.name().length();

    private static InternalNotificationRequestDTO dto(String title, String body, Map<String, String> data) {
        InternalNotificationRequestDTO dto = new InternalNotificationRequestDTO();
        dto.setAuthId(UUID.randomUUID());
        dto.setType(NotificationType.NEW_MESSAGE);
        dto.setTitle(title);
        dto.setBody(body);
        dto.setData(data);
        return dto;
    }

    @Test
    void acceptsAtLimit() {
        String body = "b".repeat(FcmPayloadSizeValidator.MAX_PAYLOAD_BYTES - FIXED - 1);
        assertThat(validator.isValid(dto("t", body, null), null)).isTrue();
    }

    @Test
    void rejectsOneByteOverLimit() {
        String body = "b".repeat(FcmPayloadSizeValidator.MAX_PAYLOAD_BYTES - FIXED);
        assertThat(validator.isValid(dto("t", body, null), null)).isFalse();
    }

    @Test
    void countsUtf8BytesNotChars() {
        // 1 char "ç" = 2 bytes; 1 emoji = 4 bytes
        int room = FcmPayloadSizeValidator.MAX_PAYLOAD_BYTES - FIXED - 1; // bytes available for body
        String twoByte = "ç".repeat(room / 2);
        assertThat(twoByte.length()).isLessThan(room);
        assertThat(validator.isValid(dto("t", twoByte, null), null)).isTrue();
        assertThat(validator.isValid(dto("t", twoByte + "ç" + "ç", null), null)).isFalse();

        String emoji = "😀".repeat(room / 4);
        assertThat(validator.isValid(dto("t", emoji, null), null)).isTrue();
        assertThat(validator.isValid(dto("t", emoji + "😀" + "😀", null), null)).isFalse();
    }

    @Test
    void countsDataKeysAndValues() {
        // title "t" (1) + body "b" (1) já consomem 2 bytes
        int dataBudget = FcmPayloadSizeValidator.MAX_PAYLOAD_BYTES - FIXED - 2;
        String value = "v".repeat(dataBudget - "key".length());
        assertThat(validator.isValid(dto("t", "b", Map.of("key", value)), null)).isTrue();
        assertThat(validator.isValid(dto("t", "b", Map.of("key", value + "v")), null)).isFalse();
    }

    @Test
    void nullFieldsCountZero() {
        assertThat(validator.isValid(new InternalNotificationRequestDTO(), null)).isTrue();
    }
}
