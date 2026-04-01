package com.example.BookWhiz.model.show;

import com.example.BookWhiz.model.movie.Movie;
import com.example.BookWhiz.model.venue.Venue;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "shows")
@Data
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    // ── Store as LocalDate / LocalTime ─────────────────────────
    // MySQL DATE column  → LocalDate  (e.g. 2025-06-15)
    // MySQL TIME column  → LocalTime  (e.g. 18:30:00)
    //
    // @JsonFormat forces Jackson to serialise as strings, NOT arrays.
    // Without this Jackson produces [2025,6,15] and [18,30,0] which
    // breaks every frontend that expects showDate/showTime as a string.

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "show_date")
    private LocalDate showDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @Column(name = "show_time")
    private LocalTime showTime;

    private Double  price;

    @Column(name = "total_seats")
    private Integer totalSeats;
}