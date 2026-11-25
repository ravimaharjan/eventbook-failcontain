package com.eventbook.booking.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class BookingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BookingEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void paymentRequested(UUID bookingId, PaymentRequestedEvent event) {
        publish("payment.requested", bookingId, event);
    }

    public void refundRequested(UUID bookingId) {
        publish("payment.refund.requested", bookingId, new RefundRequestedEvent(bookingId));
    }

    private void publish(String topic, UUID key, Object event) {
        try {
            kafkaTemplate.send(topic, key.toString(), event).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing " + topic, exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Could not publish event to " + topic, exception);
        }
    }
}
