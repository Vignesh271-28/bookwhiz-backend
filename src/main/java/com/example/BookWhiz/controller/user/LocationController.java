package com.example.BookWhiz.controller.user;

import com.example.BookWhiz.model.movie.Movie;
import com.example.BookWhiz.model.venue.Venue;
import com.example.BookWhiz.repository.MovieRepository;
import com.example.BookWhiz.repository.VenueRepository;
import com.example.BookWhiz.service.movie.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class LocationController {

    private final VenueRepository venueRepository;
    private final MovieRepository movieRepository;
    private final MovieService    movieService;

    public LocationController(VenueRepository venueRepository,
                              MovieRepository movieRepository,
                              MovieService movieService) {
        this.venueRepository = venueRepository;
        this.movieRepository = movieRepository;
        this.movieService    = movieService;
    }

    // ── City autocomplete ─────────────────────────────────────
    @GetMapping("/locations/search")
    public List<String> searchCities(@RequestParam String query) {
        return venueRepository.findAll().stream()
                .map(Venue::getCity)
                .filter(c -> c.toLowerCase().startsWith(query.toLowerCase()))
                .distinct()
                .toList();
    }

    // ── GET all movies (with optional filters) ────────────────
    @GetMapping("/movies")
    @Transactional(readOnly = true)
    public List<Movie> getMovies(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String format
    ) {
        if (city == null || city.isBlank()) {
            return movieService.getAllMovies();
        }
        return movieRepository.findMoviesByCity(city, language, genre, format);
    }

    // ── GET single movie by id ────────────────────────────────
    @GetMapping("/movies/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return movieRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }   

    // ── Theatres by city / area ───────────────────────────────
    @GetMapping("/theatres")
    public List<Venue> getTheatres(
            @RequestParam String city,
            @RequestParam(required = false) String area
    ) {
        return area == null
                ? venueRepository.findByCity(city)
                : venueRepository.findByCityAndArea(city, area);
    }

}