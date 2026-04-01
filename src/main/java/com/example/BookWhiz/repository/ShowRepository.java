package com.example.BookWhiz.repository;

import com.example.BookWhiz.model.show.Show;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("""
      SELECT s FROM Show s
      WHERE s.movie.id = :movieId
      AND s.venue.city = :city
      ORDER BY s.venue.name, s.showTime
    """)
    List<Show> findShowsByMovieAndCity(
            Long movieId,
            String city
    );



@Modifying
@Query("DELETE FROM Show s WHERE s.movie.id = :movieId")
void deleteByMovieId(@Param("movieId") Long movieId);


    List<Show> findByMovieId(Long movieId);

    // Delete all shows for a movie — called before deleting the movie
    // @Transactional
    // void deleteByMovieId(Long movieId);

    // Delete all shows for a venue — called before deleting the venue
    @Transactional
    void deleteByVenueId(Long venueId);


    @Query("SELECT s FROM Show s LEFT JOIN FETCH s.movie LEFT JOIN FETCH s.venue")
    List<Show> findAllWithDetails();


   

    
}
