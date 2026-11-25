package com.eventbook.payment.repository.query;

import com.eventbook.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QueryPaymentRepository extends JpaRepository<Payment, UUID> {
}
