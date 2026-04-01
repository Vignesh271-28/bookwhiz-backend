package com.example.BookWhiz.dto.request;


import java.util.List;

public class SeatLockRequest {

    private Long showId;
    private List<Long> seatIds;

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }
}
