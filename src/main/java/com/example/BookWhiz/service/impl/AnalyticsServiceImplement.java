package com.example.BookWhiz.service.impl;

import com.example.BookWhiz.dto.response.DashboardStatsResponse;
import com.example.BookWhiz.model.booking.BookingStatus;
import com.example.BookWhiz.model.payment.PaymentStatus;
import com.example.BookWhiz.repository.*;
import com.example.BookWhiz.service.analytics.AnalyticsService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AnalyticsServiceImplement implements AnalyticsService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ShowRepository    showRepository;
    private final VenueRepository   venueRepository;
    private final MovieRepository   movieRepository;

    public AnalyticsServiceImplement(BookingRepository bookingRepository,
                                PaymentRepository paymentRepository,
                                ShowRepository showRepository,
                                VenueRepository venueRepository,
                                MovieRepository movieRepository) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.showRepository    = showRepository;
        this.venueRepository   = venueRepository;
        this.movieRepository   = movieRepository;
    }

    @Override
    public DashboardStatsResponse getPlatformStats() {

        // ── Booking counts ────────────────────────────────────
        long totalBookings     = bookingRepository.count();
        long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long expiredBookings   = bookingRepository.countByStatus(BookingStatus.EXPIRED);
        long lockedBookings    = bookingRepository.countByStatus(BookingStatus.LOCKED);

        // ── Revenue ───────────────────────────────────────────
        double totalRevenue = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .mapToDouble(p -> p.getAmount())
                .sum();

        // Confirmed revenue = payments linked to CONFIRMED bookings
        double confirmedRevenue = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS
                          && p.getBooking() != null
                          && p.getBooking().getStatus() == BookingStatus.CONFIRMED)
                .mapToDouble(p -> p.getAmount())
                .sum();

        // Refunded = payments for CANCELLED bookings
        double refundedAmount = paymentRepository.findAll().stream()
                .filter(p -> p.getBooking() != null
                          && p.getBooking().getStatus() == BookingStatus.CANCELLED)
                .mapToDouble(p -> p.getAmount())
                .sum();

        // ── Content counts ────────────────────────────────────
        long totalEvents = showRepository.count();
        long totalVenues = venueRepository.count();
        long totalMovies = movieRepository.count();

        return new DashboardStatsResponse(
                totalBookings,
                confirmedBookings,
                cancelledBookings,
                expiredBookings,
                lockedBookings,
                totalRevenue,
                confirmedRevenue,
                refundedAmount,
                totalEvents,
                totalVenues,
                totalMovies
        );
    }

    @Override
    public Map<String, Object> getAllUsersAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalBookings",     bookingRepository.count());
        analytics.put("confirmedBookings", bookingRepository.countByStatus(BookingStatus.CONFIRMED));
        analytics.put("cancelledBookings", bookingRepository.countByStatus(BookingStatus.CANCELLED));
        analytics.put("totalRevenue", paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .mapToDouble(p -> p.getAmount()).sum());
        return analytics;
    }
}