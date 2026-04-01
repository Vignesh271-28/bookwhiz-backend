package com.example.BookWhiz.service.analytics;


import com.example.BookWhiz.dto.response.DashboardStatsResponse;

import java.util.Map;

public interface AnalyticsService {
    DashboardStatsResponse getPlatformStats();
    Map<String, Object> getAllUsersAnalytics();

}