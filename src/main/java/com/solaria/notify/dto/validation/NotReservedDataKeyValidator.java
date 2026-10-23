package com.solaria.notify.dto.validation;

import java.util.Locale;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotReservedDataKeyValidator implements ConstraintValidator<NotReservedDataKey, String> {

    private static final Set<String> RESERVED = Set.of("from", "notification", "message_type", "collapse_key",
            "notificationid", "type", "title", "body");

    @Override
    public boolean isValid(String key, ConstraintValidatorContext context) {
        if (key == null) {
            return true;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return !RESERVED.contains(lower) && !lower.startsWith("google") && !lower.startsWith("gcm");
    }
}
