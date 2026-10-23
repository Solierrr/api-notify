package com.solaria.notify.push;

// Falha do lote inteiro no provedor; a mensagem nunca contém tokens
public class PushDeliveryException extends RuntimeException {

    public PushDeliveryException(String message) {
        super(message);
    }
}
