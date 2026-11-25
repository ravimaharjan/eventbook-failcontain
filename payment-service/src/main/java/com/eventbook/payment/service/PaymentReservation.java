package com.eventbook.payment.service;

import com.eventbook.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentReservation(
        UUID id,
        UUID bookingId,
        BigDecimal amount,
        PaymentStatus status,
        boolean created) {
}
