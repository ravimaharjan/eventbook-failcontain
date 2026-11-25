package com.eventbook.payment.service;

import com.eventbook.payment.repository.query.QueryPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentQueryService {

    private final QueryPaymentRepository payments;

    public PaymentQueryService(QueryPaymentRepository payments) {
        this.payments = payments;
    }

    @Transactional(readOnly = true, transactionManager = "queryTransactionManager")
    public PaymentResponse get(UUID id) {
        return payments.findById(id)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
