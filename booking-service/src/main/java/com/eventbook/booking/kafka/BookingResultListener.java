package com.eventbook.booking.kafka;

import com.eventbook.booking.event.PaymentResultEvent;
import com.eventbook.booking.service.BookingResultService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class BookingResultListener {

    private final BookingResultService results;

    public BookingResultListener(BookingResultService results) {
        this.results = results;
    }

    @KafkaListener(id = "payment-completed-listener", topics = "payment.completed",
            containerFactory = "paymentResultKafkaListenerContainerFactory")
    public void paymentCompleted(PaymentResultEvent event, Acknowledgment acknowledgment) {
        results.paymentCompleted(event.bookingId());
        acknowledgment.acknowledge();
    }

    @KafkaListener(id = "payment-failed-listener", topics = "payment.failed",
            containerFactory = "paymentResultKafkaListenerContainerFactory")
    public void paymentFailed(PaymentResultEvent event, Acknowledgment acknowledgment) {
        results.paymentFailed(event.bookingId());
        acknowledgment.acknowledge();
    }
}
