package com.solaria.notify.dto.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// Chave de data que o FCM reserva ou que colide com os campos do próprio push
@Documented
@Constraint(validatedBy = NotReservedDataKeyValidator.class)
@Target({ ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface NotReservedDataKey {

    String message() default "chave de data reservada";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
