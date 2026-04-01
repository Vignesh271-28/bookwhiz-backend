package com.example.BookWhiz.controller.payment;

import com.example.BookWhiz.model.booking.Booking;
import com.example.BookWhiz.model.booking.BookingStatus;
import com.example.BookWhiz.model.payment.Payment;
import com.example.BookWhiz.service.booking.BookingService;
import com.example.BookWhiz.service.payment.PaymentService;
import com.example.BookWhiz.util.QrCodeGenerator;
import com.example.BookWhiz.exception.PaymentFailedException;
import org.springframework.web.bind.annotation.*;

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
     */
    @PostMapping("/initiate/{bookingId}")
    public Payment initiate(@PathVariable Long bookingId) {

        Booking booking = bookingService.getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.LOCKED) {
            throw new PaymentFailedException(
                    "Payment can be initiated only for LOCKED bookings");
        }

        return paymentService.initiatePayment(booking);
    }

    /**
     * Step 2: Confirm payment (webhook / success callback)
     */
    @PostMapping("/confirm/{paymentId}")
    public Payment confirm(@PathVariable Long paymentId) {

        Payment payment = paymentService.confirmPayment(paymentId);

        // ✅ Generate QR AFTER booking is confirmed
//        try {
//            QrCodeGenerator.generateQRCode(
//                    "BOOKING_ID:" + payment.getBooking().getId(),
//                    "qr-booking-" + payment.getBooking().getId() + ".png"
//            );
//        } catch (Exception e) {
//            throw new RuntimeException("QR generation failed");
//        }

        return payment;
    }
}
