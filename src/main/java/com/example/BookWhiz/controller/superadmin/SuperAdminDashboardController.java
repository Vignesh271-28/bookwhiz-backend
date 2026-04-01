package com.example.BookWhiz.controller.superadmin;

import com.example.BookWhiz.dto.response.DashboardStatsResponse;
import com.example.BookWhiz.service.analytics.AnalyticsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/superadmin")
public class SuperAdminDashboardController {

    private final AnalyticsService analyticsService;

    public SuperAdminDashboardController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public DashboardStatsResponse dashboard() {
        return analyticsService.getPlatformStats();
    }
}

