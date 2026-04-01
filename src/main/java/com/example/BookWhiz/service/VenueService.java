package com.example.BookWhiz.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.BookWhiz.model.venue.Venue;

import java.util.List;
import java.util.Optional;

public interface VenueService {
    List<Venue>          getAllVenues();
    Page<Venue>          getAllVenuesPaged(Pageable pageable); // ← added
    Optional<Venue>      findById(Long id);
    Venue                createVenue(Venue venue);
    Optional<Venue>      updateVenue(Long id, Venue venue);
    void                 deleteVenue(Long id);
}