package com.eventbook.payment.api;

import com.eventbook.payment.service.PaymentQueryService;
import com.eventbook.payment.service.PaymentResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PaymentController {

    private final PaymentQueryService payments;

    public PaymentController(PaymentQueryService payments) {
        this.payments = payments;
    }

    @GetMapping("/payments/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return payments.get(id);
    }
}
