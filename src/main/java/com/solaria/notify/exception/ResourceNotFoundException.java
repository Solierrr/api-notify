package com.solaria.notify.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Recurso não encontrado -> {@code 404 RESOURCE_NOT_FOUND}
 *
 * <p>
 * Lançada pelos services quando o registro pedido não existe -> o cliente decide pelo
 * {@code errorCode}, então o código fica estável mesmo que a mensagem mude
 * </p>
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message, Map<String, Object> properties) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", properties);
    }
}
