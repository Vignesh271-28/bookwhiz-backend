package com.example.BookWhiz.repository;


import com.example.BookWhiz.model.event.Event;
import com.example.BookWhiz.model.event.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("""
    SELECT e FROM Event e
    JOIN e.venue v
    WHERE e.status = 'APPROVED'
""")
    List<Event> findApprovedEventsWithVenue();

    List<Event> findByStatus(EventStatus status);
}

