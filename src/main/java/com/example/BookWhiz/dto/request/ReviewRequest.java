package com.example.BookWhiz.dto.request;

// ── Incoming request ─────────────────────────────────────────
public class ReviewRequest {
    private Long    movieId;
    private Integer rating;   // 1–5
    private String  comment;

    public Long    getMovieId() { return movieId; }
    public Integer getRating()  { return rating;  }
    public String  getComment() { return comment; }

    public void setMovieId(Long m)   { this.movieId = m; }
    public void setRating(Integer r) { this.rating  = r; }
    public void setComment(String c) { this.comment = c; }
}