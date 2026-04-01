package com.example.BookWhiz.controller.superadmin;


import com.example.BookWhiz.model.event.Event;
import com.example.BookWhiz.service.event.EventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin/events")
public class SuperAdminEventController {

    private final EventService eventService;

    public SuperAdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/pending")
    public List<Event> getPendingEvents() {
        return eventService.getPendingEvents();
    }

    @PutMapping("/{id}/approve")
    public Event approve(@PathVariable Long id) {
        return eventService.approveEvent(id);
    }

    @PutMapping("/{id}/reject")
    public Event reject(@PathVariable Long id) {
        return eventService.rejectEvent(id);
    }
}

