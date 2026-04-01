package com.example.BookWhiz.controller;

import com.example.BookWhiz.model.booking.BookingStatus;
import com.example.BookWhiz.model.show.Show;
import com.example.BookWhiz.model.venue.Seat;
import com.example.BookWhiz.model.venue.Venue;
import com.example.BookWhiz.repository.BookingRepository;
import com.example.BookWhiz.repository.SeatRepository;
import com.example.BookWhiz.repository.ShowRepository;
import com.example.BookWhiz.repository.VenueRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/user/seats")
@CrossOrigin(origins = "*")
public class SeatController {

    private final SeatRepository      seatRepository;
    private final BookingRepository   bookingRepository;
    private final ShowRepository      showRepository;
    private final VenueRepository     venueRepository;
    private final StringRedisTemplate redisTemplate;

    public SeatController(SeatRepository seatRepository,
                          BookingRepository bookingRepository,
                          ShowRepository showRepository,
                          VenueRepository venueRepository,
                          StringRedisTemplate redisTemplate) {
        this.seatRepository   = seatRepository;
        this.bookingRepository = bookingRepository;
        this.showRepository   = showRepository;
        this.venueRepository  = venueRepository;
        this.redisTemplate    = redisTemplate;
    }

    // ── GET /api/user/seats/venue/{venueId} ───────────────────
    @GetMapping("/venue/{venueId}")
    public List<Seat> getSeatsByVenue(@PathVariable Long venueId) {
        return seatRepository.findByVenueId(venueId);
    }

    // ── GET /api/user/seats/venue/{venueId}/state?showId=X ───
    @GetMapping("/venue/{venueId}/state")
    @Transactional
    public ResponseEntity<?> getSeatState(
            @PathVariable Long venueId,
            @RequestParam  Long showId
    ) {
        List<Seat> seats = seatRepository.findByVenueId(venueId);

        // ── Auto-create seats in DB if venue has none ─────────
        // This runs ONCE per new venue. After this, all seats have real IDs
        // and lockSeats will find them normally.
        if (seats.isEmpty()) {
            Show show = showRepository.findById(showId).orElse(null);
            int total = (show != null && show.getTotalSeats() != null)
                        ? show.getTotalSeats() : 80;

            Venue venue = venueRepository.findById(venueId).orElse(null);
            if (venue == null) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Venue not found: " + venueId));
            }

            seats = createAndSaveSeats(venue, total);
        }

        // ── Build state response with real IDs ────────────────
        List<Long> bookedSeatIds = getBookedSeatIds(showId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Seat seat : seats) {
            String  redisKey    = "seat-lock:" + showId + ":" + seat.getId();
            String  lockedByStr = redisTemplate.opsForValue().get(redisKey);
            boolean isLocked    = lockedByStr != null;
            Long    lockedBy    = isLocked ? parseLong(lockedByStr) : null;
            boolean isBooked    = bookedSeatIds.contains(seat.getId());
            String  category    = deriveCategoryFromRow(seat.getRowLabel());

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("seatId",     seat.getId());         // REAL DB id — lockSeats will find it
            dto.put("seatNumber", seat.getSeatNumber());
            dto.put("rowLabel",   seat.getRowLabel());
            dto.put("category",   category);
            dto.put("booked",     isBooked);
            dto.put("locked",     isLocked);
            dto.put("lockedBy",   lockedBy);
            dto.put("virtual",    false);
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }

    // ── Insert real seat rows into DB ─────────────────────────
    /**
     * Seat layout — rows start from A near the screen, Couple Den at the back:
     *
     *   ▼ SCREEN ▼
     *   Rows A–E  → REGULAR  (Standard  · 14 seats · cheapest)
     *   Rows F–I  → PREMIUM  (Premium   · 12 seats)
     *   Rows J–K  → VIP      (Gold      · 10 seats)
     *   Rows L–M  → COUPLE   (Couple Den · 10 wide seats · most expensive)
     */
    private List<Seat> createAndSaveSeats(Venue venue, int total) {
        // {rowLabel, category, seatsPerRow}
        String[][] layout = {
            {"A", "REGULAR", "14"}, {"B", "REGULAR", "14"},
            {"C", "REGULAR", "14"}, {"D", "REGULAR", "14"}, {"E", "REGULAR", "14"},
            {"F", "PREMIUM", "12"}, {"G", "PREMIUM", "12"},
            {"H", "PREMIUM", "12"}, {"I", "PREMIUM", "12"},
            {"J", "VIP",     "10"}, {"K", "VIP",     "10"},
            {"L", "COUPLE",  "10"}, {"M", "COUPLE",  "10"},
        };

        List<Seat> toSave = new ArrayList<>();
        int created = 0;

        for (String[] row : layout) {
            if (created >= total) break;
            String rowLabel = row[0];
            int    rowSize  = Math.min(Integer.parseInt(row[2]), total - created); // row[2] = size

            for (int n = 1; n <= rowSize; n++) {
                Seat seat = new Seat();
                seat.setVenue(venue);
                seat.setRowLabel(rowLabel);
                seat.setSeatNumber(rowLabel + n);
                seat.setActive(true);
                toSave.add(seat);
                created++;
            }
        }

        return seatRepository.saveAll(toSave);
    }

    // ── Helpers ───────────────────────────────────────────────
    private String deriveCategoryFromRow(String rowLabel) {
        if (rowLabel == null || rowLabel.isEmpty()) return "REGULAR";
        char r = rowLabel.toUpperCase().charAt(0);
        if (r <= 'E') return "REGULAR";  // Rows A–E → Standard (closest to screen)
        if (r <= 'I') return "PREMIUM";  // Rows F–I → Premium
        if (r <= 'K') return "VIP";      // Rows J–K → Gold
        return "COUPLE";                 // Rows L–M → Couple Den (back, most expensive)
    }

    private List<Long> getBookedSeatIds(Long showId) {
        try {
            return bookingRepository
                .findBookedSeatIdsByShowIdAndStatus(showId, BookingStatus.CONFIRMED);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static Long parseLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return null; }
    }
}