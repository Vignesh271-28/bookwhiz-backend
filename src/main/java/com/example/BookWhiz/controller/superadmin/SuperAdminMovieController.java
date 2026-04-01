package com.example.BookWhiz.controller.superadmin;

import com.example.BookWhiz.model.movie.Movie;
import com.example.BookWhiz.repository.MovieRepository;
import com.example.BookWhiz.service.movie.MovieService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/superadmin/movies")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
@CrossOrigin
public class SuperAdminMovieController {

    private final MovieService    movieService;
    private final MovieRepository movieRepository;

    public SuperAdminMovieController(MovieService movieService, MovieRepository movieRepository) {
        this.movieService    = movieService;
        this.movieRepository = movieRepository;
    }

    // GET /api/superadmin/movies?page=0&size=10
    @GetMapping
    public Page<Movie> getAllMovies(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return movieRepository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovie(@PathVariable Long id) {
        return movieRepository.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Movie> createMovie(@RequestBody Movie movie) {
        return ResponseEntity.ok(movieService.createMovie(movie));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Movie> updateMovie(@PathVariable Long id, @RequestBody Movie movie) {
        return movieService.updateMovie(id, movie).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}

