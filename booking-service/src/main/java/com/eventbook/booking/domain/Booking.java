package com.eventbook.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int seats;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Booking() {
    }

    public Booking(UUID eventId, UUID userId, int seats, BigDecimal amount) {
        this.eventId = eventId;
        this.userId = userId;
        this.seats = seats;
        this.amount = amount;
        this.status = BookingStatus.PENDING;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void confirm() {
        if (status == BookingStatus.PENDING) {
            status = BookingStatus.CONFIRMED;
        }
    }

    public void cancel() {
        if (status == BookingStatus.PENDING) {
            status = BookingStatus.CANCELLED;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getSeats() {
        return seats;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
