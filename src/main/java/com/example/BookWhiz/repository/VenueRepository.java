package com.example.BookWhiz.repository;


import com.example.BookWhiz.model.venue.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    List<Venue> findByCity(String city);

    List<Venue> findByCityAndArea(String city, String area);
}

