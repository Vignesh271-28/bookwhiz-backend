package com.example.BookWhiz.controller.superadmin;

import com.example.BookWhiz.model.venue.Venue;
import com.example.BookWhiz.service.VenueService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/superadmin/venues")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
@CrossOrigin
public class SuperAdminVenueController {

    private final VenueService    venueService;
    private final EntityManager   em;

    public SuperAdminVenueController(VenueService venueService, EntityManager em) {
        this.venueService = venueService;
        this.em           = em;
    }

    @GetMapping
    public Page<Venue> getAllVenues(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return venueService.getAllVenuesPaged(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Venue> getVenue(@PathVariable Long id) {
        return venueService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Venue> createVenue(@RequestBody Venue venue) {
        return ResponseEntity.ok(venueService.createVenue(venue));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Venue> updateVenue(@PathVariable Long id, @RequestBody Venue venue) {
        return venueService.updateVenue(id, venue)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/superadmin/venues/{id}
     *
     * Cascades in the correct order to satisfy FK constraints:
     *   booking_seats → bookings → shows → seats → venue
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteVenue(@PathVariable Long id) {

        // 1. Verify venue exists
        long venueCount = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM venue WHERE id = ?1")
                .setParameter(1, id).getSingleResult()).longValue();
        if (venueCount == 0)
            return ResponseEntity.notFound().build();

        // 2. Delete booking_seats for bookings linked to shows in this venue
        em.createNativeQuery("""
                DELETE bs FROM booking_seats bs
                INNER JOIN bookings b  ON b.id  = bs.booking_id
                INNER JOIN shows    s  ON s.id  = b.show_id
                WHERE s.venue_id = ?1
                """).setParameter(1, id).executeUpdate();

        // 3. Delete bookings linked to shows in this venue
        em.createNativeQuery("""
                DELETE b FROM bookings b
                INNER JOIN shows s ON s.id = b.show_id
                WHERE s.venue_id = ?1
                """).setParameter(1, id).executeUpdate();

        // 4. Delete shows in this venue
        em.createNativeQuery(
                "DELETE FROM shows WHERE venue_id = ?1")
                .setParameter(1, id).executeUpdate();

        // 5. Delete seats in this venue
        em.createNativeQuery(
                "DELETE FROM seats WHERE venue_id = ?1")
                .setParameter(1, id).executeUpdate();

        // 6. Finally delete the venue itself
        em.createNativeQuery(
                "DELETE FROM venue WHERE id = ?1")
                .setParameter(1, id).executeUpdate();

        return ResponseEntity.ok(Map.of(
                "message", "Venue #" + id + " and all related data deleted successfully"));
    }
}