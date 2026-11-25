package com.eventbook.payment.event;

import java.util.UUID;

public record RefundRequestedEvent(UUID bookingId) {
}
