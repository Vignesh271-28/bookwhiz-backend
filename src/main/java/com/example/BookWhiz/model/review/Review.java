package com.example.BookWhiz.model.review;

import com.example.BookWhiz.model.movie.Movie;
import com.example.BookWhiz.model.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_id"}))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(nullable = false)
    private Integer rating;            // 1–5

    @Column(nullable = false, length = 500)
    private String comment;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ── Getters & Setters ────────────────────────────────────
    public Long   getId()        { return id;        }
    public User   getUser()      { return user;      }
    public Movie  getMovie()     { return movie;     }
    public Integer getRating()   { return rating;    }
    public String  getComment()  { return comment;   }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setUser(User u)           { this.user    = u;  }
    public void setMovie(Movie m)         { this.movie   = m;  }
    public void setRating(Integer r)      { this.rating  = r;  }
    public void setComment(String c)      { this.comment = c;  }
}