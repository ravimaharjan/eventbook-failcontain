package com.eventbook.booking.repository.write;

import com.eventbook.booking.domain.Booking;
import com.eventbook.booking.domain.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingWriteRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, OffsetDateTime cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findLockedById(@Param("id") UUID id);
}
