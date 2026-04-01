package com.example.BookWhiz.service;

import com.example.BookWhiz.model.movie.Movie;

import java.util.List;
import java.util.Optional;


public interface MovieService {

   

   List<Movie> getAllMovies();
   Optional<Movie> findById(Long id);
   Movie createMovie(Movie movie);
   Optional<Movie> updateMovie(Long id, Movie movie);
   void deleteMovie(Long id);




}


// ─────────────────────────────────────────────────────────────────────────────
// ADD these method signatures to your existing MovieService interface
// at: src/main/java/com/example/BookWhiz/service/movie/MovieService.java
//
// Your existing interface already has getAllMovies() — just ADD the ones below
// ─────────────────────────────────────────────────────────────────────────────


// Add these 4 methods inside the interface:

