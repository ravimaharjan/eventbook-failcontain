package com.eventbook.booking.event;

import java.util.UUID;

public record RefundRequestedEvent(UUID bookingId) {
}
