package com.example.BookWhiz.controller.user;

import com.example.BookWhiz.model.event.Event;
import com.example.BookWhiz.service.event.EventSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/events")
public class UserEventSearchController {

    private final EventSearchService eventSearchService;

    public UserEventSearchController(EventSearchService eventSearchService) {
        this.eventSearchService = eventSearchService;
    }

    @GetMapping("/nearby")
    public List<Event> searchNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10") double radiusKm) {

        return eventSearchService.searchNearbyEvents(
                lat, lon, radiusKm);
    }
}

