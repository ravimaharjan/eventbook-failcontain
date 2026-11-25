package com.eventbook.booking.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResultEvent(UUID bookingId, BigDecimal amount, String failureReason) {
}
