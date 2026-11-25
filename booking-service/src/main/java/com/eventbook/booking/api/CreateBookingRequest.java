package com.eventbook.booking.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID eventId,
        @NotNull UUID userId,
        @Positive int seats,
        @NotNull @DecimalMin("0.01") BigDecimal amount) {
}
