package com.example.BookWhiz.controller.admin;


import com.example.BookWhiz.model.venue.Venue;
import com.example.BookWhiz.service.venue.VenueService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/venues")
public class AdminVenueController {

    private final VenueService venueService;

    public AdminVenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    public Venue createVenue(@RequestBody Venue venue) {
        return venueService.createVenue(venue);
    }
}

