package com.eventbook.payment.service;

import com.eventbook.payment.domain.PaymentStatus;
import com.eventbook.payment.event.PaymentEventPublisher;
import com.eventbook.payment.event.PaymentRequestedEvent;
import com.eventbook.payment.gateway.ChargeRequest;
import com.eventbook.payment.gateway.GatewayClient;
import com.eventbook.payment.gateway.GatewayResponse;
import com.eventbook.payment.gateway.RetryableGatewayException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class PaymentProcessor {

    private final PaymentWriteService payments;
    private final GatewayClient gateway;
    private final PaymentEventPublisher events;

    public PaymentProcessor(PaymentWriteService payments, GatewayClient gateway, PaymentEventPublisher events) {
        this.payments = payments;
        this.gateway = gateway;
        this.events = events;
    }

    public void process(PaymentRequestedEvent request) {
        PaymentReservation reservation = payments.reserve(request.bookingId(), request.amount());
        if (!reservation.created()) {
            if (reservation.status() == PaymentStatus.PROCESSING) {
                throw new RetryableGatewayException(
                        "A payment for booking " + request.bookingId() + " is still processing", null);
            }
            publishExistingResult(reservation);
            return;
        }

        GatewayResponse response;
        try {
            response = gateway.charge(new ChargeRequest(request.bookingId(), request.amount()));
        } catch (HttpClientErrorException exception) {
            payments.fail(reservation.id());
            events.failed(request.bookingId(), request.amount(), "Payment gateway rejected the charge");
            return;
        } catch (RetryableGatewayException exception) {
            payments.release(reservation.id());
            throw exception;
        }
        if (response.approved()) {
            payments.complete(reservation.id());
            events.completed(request.bookingId(), request.amount());
        } else {
            payments.fail(reservation.id());
            events.failed(request.bookingId(), request.amount(), "Payment gateway declined the charge");
        }
    }

    private void publishExistingResult(PaymentReservation payment) {
        if (payment.status() == PaymentStatus.COMPLETED) {
            events.completed(payment.bookingId(), payment.amount());
        } else if (payment.status() == PaymentStatus.FAILED) {
            events.failed(payment.bookingId(), payment.amount(), "Payment previously failed");
        }
    }
}
