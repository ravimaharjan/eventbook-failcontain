package com.eventbook.booking.service;

import com.eventbook.booking.domain.Booking;
import com.eventbook.booking.event.BookingEventPublisher;
import com.eventbook.booking.repository.result.BookingResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BookingResultService {

    private final BookingResultRepository bookings;
    private final BookingEventPublisher events;

    public BookingResultService(BookingResultRepository bookings, BookingEventPublisher events) {
        this.bookings = bookings;
        this.events = events;
    }

    @Transactional("resultConsumerTransactionManager")
    public void paymentCompleted(UUID bookingId) {
        Booking booking = bookings.findLockedById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Payment result for unknown booking " + bookingId));
        if (booking.getStatus() == com.eventbook.booking.domain.BookingStatus.CANCELLED) {
            events.refundRequested(bookingId);
            return;
        }
        booking.confirm();
    }

    @Transactional("resultConsumerTransactionManager")
    public void paymentFailed(UUID bookingId) {
        Booking booking = bookings.findLockedById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Payment result for unknown booking " + bookingId));
        booking.cancel();
    }
}
