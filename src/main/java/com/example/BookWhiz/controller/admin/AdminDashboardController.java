package com.example.BookWhiz.controller.admin;


import com.example.BookWhiz.dto.response.DashboardStatsResponse;
import com.example.BookWhiz.model.booking.Booking;
import com.example.BookWhiz.service.analytics.AnalyticsService;
import com.example.BookWhiz.service.booking.BookingService;
import com.example.BookWhiz.service.booking.BookingServiceInter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AnalyticsService analyticsService;
    private final BookingService bookingService;
    private final BookingServiceInter bookingServiceInter;

    public AdminDashboardController(
            AnalyticsService analyticsService,
            BookingService bookingService, BookingServiceInter bookingServiceInter
    ) {
        this.analyticsService = analyticsService;
        this.bookingService = bookingService;
        this.bookingServiceInter = bookingServiceInter;
    }

    // 📊 Analytics
    @GetMapping("/analytics")
    public DashboardStatsResponse getAnalytics() {
        return analyticsService.getPlatformStats();
    }

    // 📋 All bookings
    @GetMapping("/bookings")
    public List<Booking> getAllBookings() {
        return bookingServiceInter.getAllBookings();
    }
}