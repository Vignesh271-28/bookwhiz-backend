package com.example.BookWhiz.dto.response;

import java.util.List;

public class SeatUpdateMessage {

    private Long eventId;
    private List<Long> seatIds;
    private String status; // LOCKED, RELEASED, CONFIRMED

    public SeatUpdateMessage(Long eventId,
                             List<Long> seatIds,
                             String status) {
        this.eventId = eventId;
        this.seatIds = seatIds;
        this.status = status;
    }

    public Long getEventId() {
        return eventId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public String getStatus() {
        return status;
    }
}

