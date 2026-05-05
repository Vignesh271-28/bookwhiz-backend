package com.example.BookWhiz.repository;

import com.example.BookWhiz.model.payment.Payment;   // adjust package if different
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // ── Deletes payments linked to the given booking IDs
    //    Must run BEFORE bookings are deleted
    @Modifying
    @Query("DELETE FROM Payment p WHERE p.booking.id IN :bookingIds")
    void deleteByBookingIdIn(@Param("bookingIds") List<Long> bookingIds);

    // (keep all your existing methods below)
}