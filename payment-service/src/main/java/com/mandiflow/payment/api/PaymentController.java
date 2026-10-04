package com.mandiflow.payment.api;

import com.mandiflow.payment.repo.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Read-only, for debugging: did this order get charged? */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentView> byOrder(@PathVariable UUID orderId) {
        return ResponseEntity.of(paymentRepository.findByOrderId(orderId).map(PaymentView::from));
    }
}
