package com.example.BookWhiz.dto.response;

import com.example.BookWhiz.model.review.Review;

import java.time.LocalDateTime;

public class ReviewResponse {

    private Long          id;
    private Long          userId;
    private String        userName;
    private Long          movieId;
    private String        movieTitle;
    private Integer       rating;
    private String        comment;
    private LocalDateTime createdAt;

    // ── Factory ──────────────────────────────────────────────
    public static ReviewResponse from(Review r) {
        ReviewResponse dto = new ReviewResponse();
        dto.id         = r.getId();
        dto.userId     = r.getUser()  != null ? r.getUser().getId()        : null;
        dto.userName   = r.getUser()  != null ? r.getUser().getName()      : "Anonymous";
        dto.movieId    = r.getMovie() != null ? r.getMovie().getId()       : null;
        dto.movieTitle = r.getMovie() != null ? r.getMovie().getTitle()    : null;
        dto.rating     = r.getRating();
        dto.comment    = r.getComment();
        dto.createdAt  = r.getCreatedAt();
        return dto;
    }

    // ── Getters ──────────────────────────────────────────────
    public Long          getId()         { return id;         }
    public Long          getUserId()     { return userId;     }
    public String        getUserName()   { return userName;   }
    public Long          getMovieId()    { return movieId;    }
    public String        getMovieTitle() { return movieTitle; }
    public Integer       getRating()     { return rating;     }
    public String        getComment()    { return comment;    }
    public LocalDateTime getCreatedAt()  { return createdAt;  }
}