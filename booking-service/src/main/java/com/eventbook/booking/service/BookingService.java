package com.eventbook.booking.service;

import com.eventbook.booking.api.BookingResponse;
import com.eventbook.booking.api.CreateBookingRequest;
import com.eventbook.booking.domain.Booking;
import com.eventbook.booking.domain.BookingStatus;
import com.eventbook.booking.event.BookingEventPublisher;
import com.eventbook.booking.event.PaymentRequestedEvent;
import com.eventbook.booking.repository.write.BookingWriteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingWriteRepository bookings;
    private final BookingEventPublisher events;
    private final Duration holdDuration;
    private final Duration simulatedCreateDelay;

    public BookingService(
            BookingWriteRepository bookings,
            BookingEventPublisher events,
            @Value("${app.booking.hold-duration:PT10M}") Duration holdDuration,
            @Value("${app.booking.simulated-create-delay:0ms}") Duration simulatedCreateDelay) {
        if (simulatedCreateDelay.isNegative()) {
            throw new IllegalArgumentException("app.booking.simulated-create-delay must not be negative");
        }
        this.bookings = bookings;
        this.events = events;
        this.holdDuration = holdDuration;
        this.simulatedCreateDelay = simulatedCreateDelay;
    }

    public BookingResponse create(CreateBookingRequest request) {
        try {
            Thread.sleep(simulatedCreateDelay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Booking creation interrupted during simulated delay", exception);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Booking creation interrupted", exception);
        }
        Booking booking = bookings.saveAndFlush(
                new Booking(request.eventId(), request.userId(), request.seats(), request.amount()));
        events.paymentRequested(booking.getId(),
                new PaymentRequestedEvent(booking.getId(), booking.getAmount()));
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true, transactionManager = "bookingWriteTransactionManager")
    public BookingResponse get(UUID id) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Get booking interrupted during simulated delay", exception);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Get booking interrupted", exception);
        }
        return bookings.findById(id)
                .map(BookingResponse::from)
                .orElseThrow(() -> new BookingNotFoundException(id));
    }

    @Transactional("bookingWriteTransactionManager")
    public int expireStaleBookings() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minus(holdDuration);
        var staleBookings = bookings.findByStatusAndCreatedAtBefore(BookingStatus.PENDING, cutoff);
        int cancelled = 0;
        for (Booking staleBooking : staleBookings) {
            var lockedBooking = bookings.findLockedById(staleBooking.getId());
            if (lockedBooking.isPresent()
                    && lockedBooking.get().getStatus() == BookingStatus.PENDING
                    && lockedBooking.get().getCreatedAt().isBefore(cutoff)) {
                lockedBooking.get().cancel();
                cancelled++;
            }
        }
        return cancelled;
    }
}
