package com.example.BookWhiz.dto;

import java.io.Serializable;

public class SeatUpdate implements Serializable {

    private Long seatId;
    private String status; // LOCKED | UNLOCKED | BOOKED
    private Long userId;   // ONLY for LOCKED

    public SeatUpdate() {}

    public SeatUpdate(Long seatId, String status, Long userId) {
        this.seatId = seatId;
        this.status = status;
        this.userId = userId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public String getStatus() {
        return status;
    }

    public Long getUserId() {
        return userId;
    }
}