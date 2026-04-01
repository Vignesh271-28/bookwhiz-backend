package com.example.BookWhiz.controller.user;


import com.example.BookWhiz.exception.ResourceNotFoundException;
import com.example.BookWhiz.model.event.Event;
import com.example.BookWhiz.model.event.EventStatus;
import com.example.BookWhiz.repository.EventRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/events")
public class UserEventController {

    private final EventRepository eventRepository;

    public UserEventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // ✅ list page
    @GetMapping
    public List<Event> getApprovedEvents() {
        return eventRepository.findByStatus(EventStatus.APPROVED);
    }

    // ✅ details page
    @GetMapping("/{id}")
    public Event getEventById(@PathVariable Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found: " + id));
    }
}


