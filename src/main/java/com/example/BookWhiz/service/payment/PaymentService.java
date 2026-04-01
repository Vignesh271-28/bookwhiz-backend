package com.example.BookWhiz.service.payment;

import com.example.BookWhiz.exception.ResourceNotFoundException;
import com.example.BookWhiz.model.booking.Booking;
import com.example.BookWhiz.model.booking.BookingStatus;
import com.example.BookWhiz.model.payment.Payment;
import com.example.BookWhiz.model.payment.PaymentStatus;
import com.example.BookWhiz.model.venue.Seat;
import com.example.BookWhiz.repository.BookingRepository;
import com.example.BookWhiz.repository.PaymentRepository;
import com.example.BookWhiz.service.seat.SeatLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final SeatLockService seatLockService;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            SeatLockService seatLockService
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.seatLockService = seatLockService;
    }

    public Payment initiatePayment(Booking booking) {

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getSeats().size() * 200.0);
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setGatewayPaymentId(UUID.randomUUID().toString());
        payment.setCreatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment confirmPayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found: " + paymentId));

        // ✅ Idempotency check
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }

        Booking booking = payment.getBooking();

        if (booking == null) {
            throw new IllegalStateException("Payment has no booking");
        }

        if (booking.getStatus() != BookingStatus.LOCKED) {
            throw new IllegalStateException("Booking not in LOCKED state");
        }

        // 1️⃣ Update DB state
        payment.setStatus(PaymentStatus.SUCCESS);
        booking.setStatus(BookingStatus.CONFIRMED);

        bookingRepository.save(booking);
        paymentRepository.save(payment);

        // 2️⃣ Unlock Redis seats AFTER DB success
        Long showId = booking.getShow().getId();

        List<Long> seatIds = booking.getSeats()
                .stream()
                .map(Seat::getId)
                .toList();

        seatLockService.unlockSeats(showId, seatIds);

        return payment;
    }
}