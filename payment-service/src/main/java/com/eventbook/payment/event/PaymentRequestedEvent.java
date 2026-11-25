package com.eventbook.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestedEvent(UUID bookingId, BigDecimal amount) {
}
