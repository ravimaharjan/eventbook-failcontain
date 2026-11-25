package com.eventbook.booking.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestedEvent(UUID bookingId, BigDecimal amount) {
}
