package com.eventbook.payment.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void completed(UUID bookingId, java.math.BigDecimal amount) {
        publish("payment.completed", bookingId, new PaymentResultEvent(bookingId, amount, null));
    }

    public void failed(UUID bookingId, java.math.BigDecimal amount, String reason) {
        publish("payment.failed", bookingId, new PaymentResultEvent(bookingId, amount, reason));
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
