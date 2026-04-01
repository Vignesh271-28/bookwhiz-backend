package com.example.BookWhiz.service.impl;

import com.example.BookWhiz.model.venue.Venue;
import com.example.BookWhiz.repository.ShowRepository;
import com.example.BookWhiz.repository.VenueRepository;
import com.example.BookWhiz.service.VenueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final ShowRepository  showRepository;

    public VenueServiceImpl(VenueRepository venueRepository, ShowRepository showRepository) {
        this.venueRepository = venueRepository;
        this.showRepository  = showRepository;
    }

    @Override
    public List<Venue> getAllVenues() {
        return venueRepository.findAll();
    }

    // ← added for pagination
    @Override
    public Page<Venue> getAllVenuesPaged(Pageable pageable) {
        return venueRepository.findAll(pageable);
    }

    @Override
    public Optional<Venue> findById(Long id) {
        return venueRepository.findById(id);
    }

    @Override
    public Venue createVenue(Venue venue) {
        return venueRepository.save(venue);
    }

    @Override
    public Optional<Venue> updateVenue(Long id, Venue updated) {
        return venueRepository.findById(id).map(existing -> {
            if (updated.getName()       != null) existing.setName(updated.getName());
            if (updated.getCity()       != null) existing.setCity(updated.getCity());
            if (updated.getArea()       != null) existing.setArea(updated.getArea());
            if (updated.getAddress()    != null) existing.setAddress(updated.getAddress());
            if (updated.getTotalSeats() != null) existing.setTotalSeats(updated.getTotalSeats());
            return venueRepository.save(existing);
        });
    }

    @Override
    public void deleteVenue(Long id) {
        showRepository.deleteByVenueId(id);
        venueRepository.deleteById(id);
    }
}