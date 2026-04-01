package com.example.BookWhiz.dto.response;

public class DashboardStatsResponse {

    private long totalBookings;
    private double totalRevenue;
    private long totalEvents;
    private long totalUsers;

    private long   confirmedBookings;
    private long   cancelledBookings;
    private long   expiredBookings;
    private long   lockedBookings;
    private double confirmedRevenue;
    public long getConfirmedBookings() {
        return confirmedBookings;
    }

    public void setConfirmedBookings(long confirmedBookings) {
        this.confirmedBookings = confirmedBookings;
    }

    public long getCancelledBookings() {
        return cancelledBookings;
    }

    public void setCancelledBookings(long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
    }

    public long getExpiredBookings() {
        return expiredBookings;
    }

    public void setExpiredBookings(long expiredBookings) {
        this.expiredBookings = expiredBookings;
    }

    public long getLockedBookings() {
        return lockedBookings;
    }

    public void setLockedBookings(long lockedBookings) {
        this.lockedBookings = lockedBookings;
    }

    public double getConfirmedRevenue() {
        return confirmedRevenue;
    }

    public void setConfirmedRevenue(double confirmedRevenue) {
        this.confirmedRevenue = confirmedRevenue;
    }

    public double getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(double refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public long getTotalVenues() {
        return totalVenues;
    }

    public void setTotalVenues(long totalVenues) {
        this.totalVenues = totalVenues;
    }

    public long getTotalMovies() {
        return totalMovies;
    }

    public void setTotalMovies(long totalMovies) {
        this.totalMovies = totalMovies;
    }

    private double refundedAmount;
    private long   totalVenues;
    private long   totalMovies;



    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public DashboardStatsResponse(long totalBookings,
                                  double totalRevenue,
                                  long totalEvents, long expiredBookings2, long lockedBookings2, double totalRevenue2, double confirmedRevenue2, double refundedAmount2, long totalEvents2, long totalVenues2, long totalMovies2) {
        this.totalBookings = totalBookings;
        this.totalRevenue = totalRevenue;
        this.totalEvents = totalEvents;
        this.totalUsers = totalUsers;
    }

}

