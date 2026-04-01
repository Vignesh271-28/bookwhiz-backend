package com.example.BookWhiz.repository;

import com.example.BookWhiz.model.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // All reviews for a movie, newest first
    List<Review> findByMovieIdOrderByCreatedAtDesc(Long movieId);

    // Has this user already reviewed this movie?
    Optional<Review> findByUserIdAndMovieId(Long userId, Long movieId);

    // All reviews by a user
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Count reviews for a movie
    long countByMovieId(Long movieId);

    @Modifying
@Query("DELETE FROM Review r WHERE r.movie.id = :movieId")
void deleteByMovieId(@Param("movieId") Long movieId);
}