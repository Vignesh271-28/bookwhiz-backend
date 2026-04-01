package com.example.BookWhiz.dto;

public class SeatStateDTO {
    private Long seatId;
    private String seatNumber;
    private String rowLabel;
    private boolean booked;      // ← ADD THIS — was missing
    private boolean locked;
    private Long lockedBy;

    public SeatStateDTO() {}

    public SeatStateDTO(Long seatId, String seatNumber, String rowLabel,
                        boolean booked, boolean locked, Long lockedBy) {
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.rowLabel = rowLabel;
        this.booked = booked;
        this.locked = locked;
        this.lockedBy = lockedBy;
    }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

    public boolean isBooked() { return booked; }
    public void setBooked(boolean booked) { this.booked = booked; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public Long getLockedBy() { return lockedBy; }
    public void setLockedBy(Long lockedBy) { this.lockedBy = lockedBy; }
}