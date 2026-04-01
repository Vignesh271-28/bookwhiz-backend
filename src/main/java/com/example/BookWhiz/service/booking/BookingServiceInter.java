package com.example.BookWhiz.service.booking;

import com.example.BookWhiz.model.booking.Booking;

import java.util.List;

public interface BookingServiceInter {

    Booking getBookingById(Long bookingId);

    List<Booking> getAllBookings();                 // Manager / Admin

    Booking confirmBooking(Long bookingId);         // After payment success

    Booking cancelBooking(Long bookingId);          // User / Admin

    Booking expireBooking(Long bookingId);        // User / Admin
}