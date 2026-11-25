package com.eventbook.payment.service;

import com.eventbook.payment.domain.Payment;
import com.eventbook.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID bookingId, BigDecimal amount, PaymentStatus status,
                              OffsetDateTime createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getBookingId(), payment.getAmount(),
                payment.getStatus(), payment.getCreatedAt());
    }
}
