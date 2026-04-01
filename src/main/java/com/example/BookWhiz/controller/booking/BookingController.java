package com.example.BookWhiz.controller.booking;

import com.example.BookWhiz.exception.ResourceNotFoundException;
import com.example.BookWhiz.model.booking.Booking;
import com.example.BookWhiz.model.booking.BookingStatus;
import com.example.BookWhiz.model.show.Show;
import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.model.venue.Seat;
import com.example.BookWhiz.repository.*;
import com.example.BookWhiz.service.booking.BookingService;
import com.example.BookWhiz.service.seat.SeatLockService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService    bookingService;
    private final ShowRepository    showRepository;
    private final EventRepository   eventRepository;
    private final UserRepository    userRepository;
    private final SeatRepository    seatRepository;
    private final BookingRepository bookingRepository;
    private final SeatLockService   seatLockService;
    private final EntityManager     em;

    public BookingController(BookingService bookingService,
                             ShowRepository showRepository,
                             EventRepository eventRepository,
                             UserRepository userRepository,
                             SeatRepository seatRepository,
                             BookingRepository bookingRepository,
                             SeatLockService seatLockService,
                             EntityManager em) {
        this.bookingService    = bookingService;
        this.showRepository    = showRepository;
        this.eventRepository   = eventRepository;
        this.userRepository    = userRepository;
        this.seatRepository    = seatRepository;
        this.bookingRepository = bookingRepository;
        this.seatLockService   = seatLockService;
        this.em                = em;
    }

    // ── GET /api/bookings/{id} ────────────────────────────────
    // Returns a flat Map — no circular reference, no LazyInitException
    @GetMapping("/{id}")
    @Transactional
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getBookingById(@PathVariable Long id) {

        // Native SQL — same safe pattern used by AdminBookingController
        List<Object[]> rows = em.createNativeQuery("""
            SELECT
                b.id           AS bookingId,
                b.status       AS status,
                b.total_price  AS totalPrice,
                b.locked_at    AS lockedAt,
                b.confirmed_at AS confirmedAt,
                u.id           AS userId,
                u.name         AS userName,
                u.email        AS userEmail,
                sh.id          AS showId,
                sh.show_date   AS showDate,
                sh.show_time   AS showTime,
                sh.price       AS showPrice,
                m.id           AS movieId,
                m.title        AS movieTitle,
                m.poster_url   AS moviePosterUrl,
                v.id           AS venueId,
                v.name         AS venueName,
                v.city         AS venueCity,
                v.area         AS venueArea
            FROM bookings b
            LEFT JOIN users  u  ON u.id  = b.user_id
            LEFT JOIN shows  sh ON sh.id = b.show_id
            LEFT JOIN movies m  ON m.id  = sh.movie_id
            LEFT JOIN venue  v  ON v.id  = sh.venue_id
            WHERE b.id = ?1
            """).setParameter(1, id).getResultList();

        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Object[] r = rows.get(0);
        Long bookingId = toLong(r[0]);

        // Seats
        List<Object[]> seatRows = em.createNativeQuery("""
            SELECT s.id, s.seat_number, s.row_label
            FROM booking_seats bs
            JOIN seats s ON s.id = bs.seat_id
            WHERE bs.booking_id = ?1
            """).setParameter(1, bookingId).getResultList();

        List<Map<String, Object>> seats = new ArrayList<>();
        for (Object[] sr : seatRows) {
            Map<String, Object> seat = new LinkedHashMap<>();
            seat.put("id",          toLong(sr[0]));
            seat.put("seatNumber",  str(sr[1]));
            seat.put("rowLabel",    str(sr[2]));
            seat.put("category",    deriveCategoryFromRow(str(sr[2])));
            seats.add(seat);
        }

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id",            bookingId);
        dto.put("status",        str(r[1]));
        dto.put("totalPrice",    r[2]);
        dto.put("lockedAt",      str(r[3]));
        dto.put("confirmedAt",   str(r[4]));
        dto.put("userId",        toLong(r[5]));
        dto.put("userName",      str(r[6]));
        dto.put("userEmail",     str(r[7]));
        dto.put("showId",        toLong(r[8]));
        dto.put("showDate",      str(r[9]));
        dto.put("showTime",      str(r[10]));
        dto.put("showPrice",     r[11]);
        dto.put("movieId",       toLong(r[12]));
        dto.put("movieTitle",    str(r[13]));
        dto.put("moviePosterUrl",str(r[14]));
        dto.put("venueId",       toLong(r[15]));
        dto.put("venueName",     str(r[16]));
        dto.put("venueCity",     str(r[17]));
        dto.put("venueArea",     str(r[18]));
        dto.put("seats",         seats);

        return ResponseEntity.ok(dto);
    }

    // ── GET /api/bookings/my ──────────────────────────────────
    @GetMapping("/my")
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> myBookings() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        List<Object[]> rows = em.createNativeQuery("""
            SELECT
                b.id, b.status, b.total_price, b.locked_at, b.confirmed_at,
                sh.show_date, sh.show_time, sh.price,
                m.title, m.poster_url,
                v.name, v.city, v.area
            FROM bookings b
            LEFT JOIN shows  sh ON sh.id = b.show_id
            LEFT JOIN movies m  ON m.id  = sh.movie_id
            LEFT JOIN venue  v  ON v.id  = sh.venue_id
            WHERE b.user_id = ?1
            ORDER BY b.id DESC
            """).setParameter(1, user.getId()).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Long bookingId = toLong(r[0]);

            List<Object[]> seatRows = em.createNativeQuery("""
                SELECT s.id, s.seat_number, s.row_label
                FROM booking_seats bs JOIN seats s ON s.id = bs.seat_id
                WHERE bs.booking_id = ?1
                """).setParameter(1, bookingId).getResultList();

            List<Map<String, Object>> seats = new ArrayList<>();
            for (Object[] sr : seatRows) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("id",         toLong(sr[0]));
                s.put("seatNumber", str(sr[1]));
                s.put("rowLabel",   str(sr[2]));
                seats.add(s);
            }

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id",            bookingId);
            dto.put("status",        str(r[1]));
            dto.put("totalPrice",    r[2]);
            dto.put("lockedAt",      str(r[3]));
            dto.put("confirmedAt",   str(r[4]));
            dto.put("showDate",      str(r[5]));
            dto.put("showTime",      str(r[6]));
            dto.put("showPrice",     r[7]);
            dto.put("movieTitle",    str(r[8]));
            dto.put("moviePosterUrl",str(r[9]));
            dto.put("venueName",     str(r[10]));
            dto.put("venueCity",     str(r[11]));
            dto.put("venueArea",     str(r[12]));
            dto.put("seats",         seats);
            result.add(dto);
        }
        return result;
    }

    // ── POST /api/bookings/lock/{showId} ─────────────────────
    @PostMapping("/lock/{showId}")
    @Transactional
    public ResponseEntity<?> lockSeats(@PathVariable Long showId,
                                       @RequestBody List<Long> seatIds) {

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found"));

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Long> bookedSeatIds = bookingRepository
                .findBookedSeatIdsByShowIdAndStatus(showId, BookingStatus.CONFIRMED);
        List<Long> alreadyBooked = seatIds.stream()
                .filter(bookedSeatIds::contains).toList();
        if (!alreadyBooked.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seats already booked: " + alreadyBooked);
        }

        boolean locked = seatLockService.lockSeatsAtomically(showId, seatIds, user.getId());
        if (!locked) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "One or more seats already locked");
        }

        List<Seat> seats = seatIds.stream()
                .map(sid -> seatRepository.findById(sid)
                        .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + sid)))
                .toList();

        // Calculate total price: base price × per-seat category multiplier
        double basePrice = show.getPrice() != null ? show.getPrice() : 0.0;
        double totalPrice = 0.0;
        for (Seat seat : seats) {
            String category   = deriveCategoryFromRow(seat.getRowLabel());
            double multiplier = getCategoryMultiplier(category);
            totalPrice += Math.round(basePrice * multiplier);
        }

        Booking booking = new Booking();
        booking.setShow(show);
        booking.setUser(user);
        booking.setSeats(seats);
        booking.setStatus(BookingStatus.LOCKED);
        booking.setLockedAt(LocalDateTime.now());
        booking.setTotalPrice(totalPrice);   // ← persists to total_price column

        Booking saved = bookingRepository.save(booking);

        // Return flat map to avoid circular reference on save response
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id",         saved.getId());
        resp.put("status",     saved.getStatus().name());
        resp.put("lockedAt",   saved.getLockedAt() != null ? saved.getLockedAt().toString() : null);
        resp.put("showId",     showId);
        resp.put("seatIds",    seatIds);
        resp.put("totalPrice", totalPrice);
        return ResponseEntity.ok(resp);
    }

    // ── POST /api/bookings/{id}/confirm ──────────────────────
    @PostMapping("/{id}/confirm")
    @Transactional
    public ResponseEntity<?> confirm(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());  // ← needed for monthly revenue chart

        // Ensure total_price is set (backfill if booking was created before the fix)
        if (booking.getTotalPrice() == null && booking.getShow() != null) {
            int seatCount = booking.getSeats() != null ? booking.getSeats().size() : 0;
            double price  = booking.getShow().getPrice() != null ? booking.getShow().getPrice() : 0.0;
            booking.setTotalPrice(price * seatCount);
        }

        bookingRepository.save(booking);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id",          booking.getId());
        resp.put("status",      booking.getStatus().name());
        resp.put("confirmedAt", booking.getConfirmedAt().toString());
        resp.put("totalPrice",  booking.getTotalPrice());
        return ResponseEntity.ok(resp);
    }

    // ── DELETE /api/bookings/{id}/cancel ─────────────────────
    @DeleteMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to cancel this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Already cancelled");
        if (booking.getStatus() == BookingStatus.EXPIRED)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Already expired");

        if (booking.getLockedAt() != null) {
            if (LocalDateTime.now().isAfter(booking.getLockedAt().plusMinutes(30))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cancellation window has closed (30 mins exceeded)");
            }
        }

        Long showId        = booking.getShow().getId();
        List<Long> seatIds = booking.getSeats().stream().map(Seat::getId).toList();
        seatLockService.unlockSeats(showId, seatIds);

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        return ResponseEntity.ok("Booking cancelled successfully");
    }

    // ── Helpers ───────────────────────────────────────────────
    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(o.toString());
    }
    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }
    /** Derives category from row label — mirrors SeatController.deriveCategoryFromRow */
    private static String deriveCategoryFromRow(String rowLabel) {
        if (rowLabel == null || rowLabel.isEmpty()) return "REGULAR";
        char r = rowLabel.toUpperCase().charAt(0);
        if (r <= 'E') return "REGULAR";
        if (r <= 'I') return "PREMIUM";
        if (r <= 'K') return "VIP";
        return "COUPLE";
    }

    /** Returns the price multiplier for a seat category — mirrors SeatGrid.jsx */
    private static double getCategoryMultiplier(String category) {
        if (category == null) return 1.0;
        return switch (category.toUpperCase()) {
            case "PREMIUM" -> 1.5;
            case "VIP"     -> 2.0;
            case "COUPLE"  -> 2.5;
            default        -> 1.0;   // REGULAR
        };
    }

}