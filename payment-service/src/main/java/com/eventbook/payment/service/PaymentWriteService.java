package com.eventbook.payment.service;

import com.eventbook.payment.domain.Payment;
import com.eventbook.payment.domain.PaymentStatus;
import com.eventbook.payment.repository.charge.ChargePaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentWriteService {

    private final ChargePaymentRepository payments;

    public PaymentWriteService(ChargePaymentRepository payments) {
        this.payments = payments;
    }

    @Transactional("chargeTransactionManager")
    public PaymentReservation reserve(UUID bookingId, BigDecimal amount) {
        var existing = payments.findByBookingId(bookingId);
        if (existing.isPresent()) {
            return reservation(existing.get(), false);
        }
        return reservation(payments.saveAndFlush(new Payment(bookingId, amount)), true);
    }

    @Transactional("chargeTransactionManager")
    public void complete(UUID paymentId) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment reservation disappeared: " + paymentId));
        payment.complete();
    }

    @Transactional("chargeTransactionManager")
    public void fail(UUID paymentId) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment reservation disappeared: " + paymentId));
        payment.fail();
    }

    @Transactional("chargeTransactionManager")
    public void release(UUID paymentId) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment reservation disappeared: " + paymentId));
        if (payment.getStatus() == PaymentStatus.PROCESSING) {
            payments.delete(payment);
        }
    }

    private PaymentReservation reservation(Payment payment, boolean created) {
        return new PaymentReservation(payment.getId(), payment.getBookingId(), payment.getAmount(),
                payment.getStatus(), created);
    }
}
