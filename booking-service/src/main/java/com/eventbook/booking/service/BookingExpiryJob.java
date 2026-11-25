package com.eventbook.booking.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingExpiryJob {

    private final BookingService bookings;

    public BookingExpiryJob(BookingService bookings) {
        this.bookings = bookings;
    }

    @Scheduled(fixedDelayString = "${app.booking.expiry-scan-delay:30s}")
    public void expireStaleBookings() {
        bookings.expireStaleBookings();
    }
}
