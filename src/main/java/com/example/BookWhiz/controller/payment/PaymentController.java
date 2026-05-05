package com.example.BookWhiz.controller.payment;

import com.example.BookWhiz.model.booking.Booking;
import com.example.BookWhiz.model.booking.BookingStatus;
import com.example.BookWhiz.model.payment.Payment;
import com.example.BookWhiz.service.booking.BookingService;
import com.example.BookWhiz.service.payment.PaymentService;
import com.example.BookWhiz.exception.PaymentFailedException;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;

    public PaymentController(PaymentService paymentService,
                             BookingService bookingService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
    }

    /**
     * Step 1: Initiate payment (booking must be LOCKED)
     * Returns a flat DTO — never the entity tree (avoids LazyInitializationException).
     */
    @PostMapping("/initiate/{bookingId}")
    @Transactional
    public ResponseEntity<?> initiate(@PathVariable Long bookingId) {

        Booking booking = bookingService.getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.LOCKED) {
            throw new PaymentFailedException(
                    "Payment can be initiated only for LOCKED bookings");
        }

        Payment payment = paymentService.initiatePayment(booking);

        // ✅ Return a flat DTO — never serialize the entity graph
        return ResponseEntity.ok(toDto(payment));
    }

    /**
     * Step 2: Confirm payment (webhook / success callback)
     */
    @PostMapping("/confirm/{paymentId}")
    @Transactional
    public ResponseEntity<?> confirm(@PathVariable Long paymentId) {

        Payment payment = paymentService.confirmPayment(paymentId);
        return ResponseEntity.ok(toDto(payment));
    }

    // ── Safe DTO — only flat scalar fields ────────────────────
    private Map<String, Object> toDto(Payment payment) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id",               payment.getId());
        dto.put("bookingId",        payment.getBooking() != null
                                        ? payment.getBooking().getId() : null);
        dto.put("amount",           payment.getAmount());
        dto.put("status",           payment.getStatus() != null
                                        ? payment.getStatus().name() : null);
        dto.put("gatewayPaymentId", payment.getGatewayPaymentId());
        dto.put("createdAt",        payment.getCreatedAt());
        return dto;
    }
}