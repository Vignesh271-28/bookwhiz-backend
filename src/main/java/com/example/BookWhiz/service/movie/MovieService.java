package com.example.BookWhiz.service.movie;

import com.example.BookWhiz.model.movie.Movie;

import java.util.List;
import java.util.Optional;

public interface MovieService {

    // ── Already existed ──────────────────────────────────────
    List<Movie> getAllMovies();

    // ── Newly added — required by SuperAdminMovieController ──
    Optional<Movie> findById(Long id);

    Movie createMovie(Movie movie);

    Optional<Movie> updateMovie(Long id, Movie updated);

    void deleteMovie(Long id);
}