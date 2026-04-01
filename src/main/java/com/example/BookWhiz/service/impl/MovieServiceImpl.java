package com.example.BookWhiz.service.impl;

import com.example.BookWhiz.model.movie.Movie;
import com.example.BookWhiz.model.show.Show;
import com.example.BookWhiz.repository.*;
import com.example.BookWhiz.service.movie.MovieService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository   movieRepository;
    private final ShowRepository    showRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository  reviewRepository;
    private final PaymentRepository paymentRepository;  // ← added

    public MovieServiceImpl(MovieRepository   movieRepository,
                            ShowRepository    showRepository,
                            BookingRepository bookingRepository,
                            ReviewRepository  reviewRepository,
                            PaymentRepository paymentRepository) {
        this.movieRepository   = movieRepository;
        this.showRepository    = showRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository  = reviewRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    @Override
    public Optional<Movie> findById(Long id) {
        return movieRepository.findById(id);
    }

    @Override
    public Movie createMovie(Movie movie) {
        return movieRepository.save(movie);
    }

    @Override
    public Optional<Movie> updateMovie(Long id, Movie updated) {
        return movieRepository.findById(id).map(existing -> {
            if (updated.getTitle()       != null) existing.setTitle(updated.getTitle());
            if (updated.getGenre()       != null) existing.setGenre(updated.getGenre());
            if (updated.getLanguage()    != null) existing.setLanguage(updated.getLanguage());
            if (updated.getFormat()      != null) existing.setFormat(updated.getFormat());
            if (updated.getDuration()    != null) existing.setDuration(updated.getDuration());
            if (updated.getDirector()    != null) existing.setDirector(updated.getDirector());
            if (updated.getCast()        != null) existing.setCast(updated.getCast());
            if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
            if (updated.getReleaseDate() != null) existing.setReleaseDate(updated.getReleaseDate());
            if (updated.getRating()      != null) existing.setRating(updated.getRating());
            existing.setPosterUrl(updated.getPosterUrl());
            return movieRepository.save(existing);
        });
    }

    // ── DELETE — full FK-safe cascade order ──────────────────
    //
    //  payments → booking_seats → bookings → shows → reviews → movie
    //
    @Override
    @Transactional
    public void deleteMovie(Long id) {
    
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Movie not found: " + id));
    
        // 1️⃣ Get all shows
        List<Show> shows = showRepository.findByMovieId(id);
    
        List<Long> showIds = shows.stream()
                .map(Show::getId)
                .toList();
    
        List<Long> bookingIds = showIds.isEmpty()
                ? List.of()
                : bookingRepository.findIdsByShowIdIn(showIds);
    
        // 2️⃣ Delete payments
        if (!bookingIds.isEmpty()) {
            paymentRepository.deleteByBookingIdIn(bookingIds);
        }
    
        // 3️⃣ Delete bookings
        if (!showIds.isEmpty()) {
            bookingRepository.deleteByShowIdIn(showIds);
        }
    
        // 🔥🔥🔥 CRITICAL FIX
        if (!shows.isEmpty()) {
            shows.forEach(show -> show.setMovie(null)); // break relation
            showRepository.saveAll(shows);
        }
    
        // 4️⃣ Delete shows
        showRepository.deleteAll(shows);
    
        // 5️⃣ Delete reviews
        reviewRepository.deleteByMovieId(id);
    
        // 6️⃣ Delete movie
        movieRepository.delete(movie);
    }
}