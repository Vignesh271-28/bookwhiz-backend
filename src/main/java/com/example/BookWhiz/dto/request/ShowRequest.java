package com.example.BookWhiz.dto.request;

import lombok.Data;

@Data
public class ShowRequest {
    private Long   movieId;
    private Long   venueId;
    private String showDate;   // "2025-06-15"
    private String showTime;   // "18:30"
    private Double price;
    private Integer totalSeats;
}