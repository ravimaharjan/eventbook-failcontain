package com.eventbook.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public record ChargeRequest(UUID bookingId, BigDecimal amount) {
}
