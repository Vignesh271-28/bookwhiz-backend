package com.example.BookWhiz.controller.manager;

import com.example.BookWhiz.model.booking.Booking;
import com.example.BookWhiz.service.analytics.AnalyticsService;
import com.example.BookWhiz.service.booking.BookingService;
import com.example.BookWhiz.service.booking.BookingServiceInter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/dashboard")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerDashboardController {

    private final AnalyticsService analyticsService;
    private final BookingService bookingService;
    private final BookingServiceInter bookingServiceInter;

    public ManagerDashboardController(
            AnalyticsService analyticsService,
            BookingService bookingService, BookingServiceInter bookingServiceInter
    ) {
        this.analyticsService = analyticsService;
        this.bookingService = bookingService;
        this.bookingServiceInter = bookingServiceInter;
    }

    /* 🔹 Analytics */
    @GetMapping("/analytics")
    public Map<String, Object> getAllUsersAnalytics() {
        return analyticsService.getAllUsersAnalytics();
    }

    /* 🔹 All bookings */
    @GetMapping("/bookings")
    public List<Booking> getAllBookings() {
        return bookingServiceInter.getAllBookings();
    }
}