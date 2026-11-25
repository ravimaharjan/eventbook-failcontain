package com.eventbook.payment.gateway;

public class RetryableGatewayException extends RuntimeException {

    public RetryableGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
