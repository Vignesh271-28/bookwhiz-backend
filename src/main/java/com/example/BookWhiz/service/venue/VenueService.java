package com.example.BookWhiz.service.venue;

import com.example.BookWhiz.model.venue.Venue;
import com.example.BookWhiz.repository.VenueRepository;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public Venue createVenue(Venue venue) {
        return venueRepository.save(venue);
    }

    public Page<Venue> updateVenue(Long id, Venue venue) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateVenue'");
    }
    
}

