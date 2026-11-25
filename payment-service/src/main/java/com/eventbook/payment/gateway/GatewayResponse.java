package com.eventbook.payment.gateway;

public record GatewayResponse(boolean approved, String transactionId) {
}
