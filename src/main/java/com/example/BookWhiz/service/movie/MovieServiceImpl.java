// package com.example.BookWhiz.service.movie;

// import com.example.BookWhiz.model.movie.Movie;
// import com.example.BookWhiz.repository.MovieRepository;
// import org.springframework.cache.annotation.Cacheable;
// import org.springframework.stereotype.Service;

// import java.util.List;

// @Service
// public class MovieServiceImpl implements MovieService {

//     private final MovieRepository movieRepository;

//     public MovieServiceImpl(MovieRepository movieRepository) {
//         this.movieRepository = movieRepository;
//     }

// //    @Override
// //    @Cacheable("movies")
// //    public List<Movie> getAllMovies() {
// //        System.out.println("🔥 DB HIT - fetching movies from DB");
// //        return movieRepository.findAll();
// //    }

//     @Override
//     public List<Movie> getAllMovies() {
//         List<Movie> movies = movieRepository.findAll();
//         System.out.println("🎬 Total movies found: " + movies.size()); // ← add this
//         return movies;
//     }



// }