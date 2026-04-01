package com.example.BookWhiz.service.event;

import com.example.BookWhiz.model.event.Event;
import com.example.BookWhiz.model.venue.Venue;
import com.example.BookWhiz.repository.EventRepository;
import com.example.BookWhiz.util.GeoLocationUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventSearchService {

    private final EventRepository eventRepository;

    public EventSearchService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> searchNearbyEvents(
            double userLat,
            double userLon,
            double radiusKm) {

        List<Event> events =
                eventRepository.findApprovedEventsWithVenue();

        return events.stream()
                .filter(event -> {
                    Venue v = event.getVenue();
                    double distance = GeoLocationUtil.calculateDistance(
                            userLat, userLon,
                            v.getLongitude(), v.getLongitude()
                    );
                    return distance <= radiusKm;
                })
                .collect(Collectors.toList());
    }
}

