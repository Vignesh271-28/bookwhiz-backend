package com.example.BookWhiz.repository;

import com.example.BookWhiz.model.movie.Movie;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
public interface MovieRepository extends JpaRepository<Movie, Long> {

    // ✅ Join from Show side since Movie doesn't have a shows collection
    @Query("""
        SELECT DISTINCT m FROM Show s
        JOIN s.movie m
        JOIN s.venue v
        WHERE v.city = :city
        AND (:language IS NULL OR m.language = :language)
        AND (:genre    IS NULL OR m.genre    = :genre)
        AND (:format   IS NULL OR m.format   = :format)
    """)
    List<Movie> findMoviesByCity(
            @Param("city")     String city,
            @Param("language") String language,
            @Param("genre")    String genre,
            @Param("format")   String format
    );
}