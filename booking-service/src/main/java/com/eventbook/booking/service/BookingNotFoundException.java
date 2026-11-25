package com.eventbook.booking.service;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(UUID id) {
        super("Booking not found: " + id);
    }
}
