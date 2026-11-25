package com.eventbook.payment.kafka;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

@Component
public class GatewayCircuitBreakerListener {

    private final CircuitBreakerRegistry circuitBreakers;
    private final KafkaListenerEndpointRegistry listeners;

    public GatewayCircuitBreakerListener(
            CircuitBreakerRegistry circuitBreakers,
            KafkaListenerEndpointRegistry listeners) {
        this.circuitBreakers = circuitBreakers;
        this.listeners = listeners;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void observeGatewayState() {
        var breaker = circuitBreakers.circuitBreaker("gatewayCharge");
        breaker.getEventPublisher().onStateTransition(event -> {
            if (event.getStateTransition().getToState() == CircuitBreaker.State.OPEN) {
                pauseChargeListener();
            } else {
                resumeChargeListener();
            }
        });
        if (breaker.getState() == CircuitBreaker.State.OPEN) {
            pauseChargeListener();
        }
    }

    private void pauseChargeListener() {
        var container = listeners.getListenerContainer("charge-listener");
        if (container != null) {
            container.pause();
        }
    }

    private void resumeChargeListener() {
        var container = listeners.getListenerContainer("charge-listener");
        if (container != null) {
            container.resume();
        }
    }
}
