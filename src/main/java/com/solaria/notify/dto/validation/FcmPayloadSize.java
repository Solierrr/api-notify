package com.solaria.notify.dto.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// Soma dos bytes UTF-8 do payload de dados do FCM (chaves fixas + campos + data) dentro do limite de envio
@Documented
@Constraint(validatedBy = FcmPayloadSizeValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface FcmPayloadSize {

    String message() default "payload excede o limite de envio do FCM";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
