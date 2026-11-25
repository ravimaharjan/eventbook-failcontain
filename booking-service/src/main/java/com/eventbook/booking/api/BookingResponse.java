package com.eventbook.booking.api;

import com.eventbook.booking.domain.Booking;
import com.eventbook.booking.domain.BookingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID eventId,
        UUID userId,
        int seats,
        BigDecimal amount,
        BookingStatus status,
        OffsetDateTime createdAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getEventId(), booking.getUserId(),
                booking.getSeats(), booking.getAmount(), booking.getStatus(), booking.getCreatedAt());
    }
}
