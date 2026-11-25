package com.eventbook.payment.kafka;

import com.eventbook.payment.event.PaymentRequestedEvent;
import com.eventbook.payment.event.RefundRequestedEvent;
import com.eventbook.payment.service.PaymentProcessor;
import com.eventbook.payment.service.RefundProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class PaymentListener {

    private final PaymentProcessor payments;
    private final RefundProcessor refunds;

    public PaymentListener(PaymentProcessor payments, RefundProcessor refunds) {
        this.payments = payments;
        this.refunds = refunds;
    }

    @KafkaListener(id = "charge-listener", topics = "payment.requested",
            containerFactory = "chargeKafkaListenerContainerFactory")
    public void charge(PaymentRequestedEvent event, Acknowledgment acknowledgment) {
        payments.process(event);
        acknowledgment.acknowledge();
    }

    @KafkaListener(id = "refund-listener", topics = "payment.refund.requested",
            containerFactory = "refundKafkaListenerContainerFactory")
    public void refund(RefundRequestedEvent event, Acknowledgment acknowledgment) {
        refunds.process(event);
        acknowledgment.acknowledge();
    }
}
