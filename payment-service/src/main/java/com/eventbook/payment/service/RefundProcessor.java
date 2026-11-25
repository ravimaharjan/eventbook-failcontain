package com.eventbook.payment.service;

import com.eventbook.payment.domain.PaymentStatus;
import com.eventbook.payment.event.RefundRequestedEvent;
import com.eventbook.payment.repository.charge.ChargePaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundProcessor {

    private final ChargePaymentRepository payments;

    public RefundProcessor(ChargePaymentRepository payments) {
        this.payments = payments;
    }

    @Transactional("chargeTransactionManager")
    public void process(RefundRequestedEvent event) {
        payments.findByBookingId(event.bookingId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                payment.refund();
            }
        });
    }
}
