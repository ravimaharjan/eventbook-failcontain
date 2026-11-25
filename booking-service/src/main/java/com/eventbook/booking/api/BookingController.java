package com.eventbook.booking.api;

import com.eventbook.booking.service.BookingService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookings;

    public BookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Bulkhead(name = "createBooking", type = Bulkhead.Type.SEMAPHORE)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return bookings.create(request);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable UUID id) {
        return bookings.get(id);
    }
}
