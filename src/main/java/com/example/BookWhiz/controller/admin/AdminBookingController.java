package com.example.BookWhiz.controller.admin;

import com.example.BookWhiz.exception.ResourceNotFoundException;
import com.example.BookWhiz.model.booking.Booking;
import com.example.BookWhiz.model.booking.BookingStatus;
import com.example.BookWhiz.model.venue.Seat;
import com.example.BookWhiz.repository.BookingRepository;
import com.example.BookWhiz.service.seat.SeatLockService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/bookings")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@CrossOrigin(origins = "*")
public class AdminBookingController {

    private final BookingRepository bookingRepository;
    private final SeatLockService   seatLockService;
    private final EntityManager     em;

    public AdminBookingController(BookingRepository bookingRepository,
                                  SeatLockService seatLockService,
                                  EntityManager em) {
        this.bookingRepository = bookingRepository;
        this.seatLockService   = seatLockService;
        this.em                = em;
    }

    // ── GET /api/admin/bookings ───────────────────────────────
    // Uses native SQL so we don't depend on JPQL field names at all.
    // Returns plain Maps → Jackson serializes them without circular refs.
    @GetMapping
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllBookings() {

        // 1. Native SQL — pull every column we need in one shot
        List<Object[]> rows = em.createNativeQuery("""
            SELECT
                b.id           AS bookingId,
                b.status       AS status,
                b.total_price  AS totalPrice,
                b.locked_at    AS lockedAt,
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
            LEFT JOIN users         u  ON u.id  = b.user_id
            LEFT JOIN shows         sh ON sh.id = b.show_id
            LEFT JOIN movies        m  ON m.id  = sh.movie_id
            LEFT JOIN venue          v  ON v.id  = sh.venue_id
            ORDER BY b.id DESC
            """).getResultList();

        // 2. Build seats lookup: bookingId → list of seat maps
        List<Object[]> seatRows = em.createNativeQuery("""
            SELECT bs.booking_id, s.id, s.seat_number
            FROM booking_seats bs
            JOIN seats s ON s.id = bs.seat_id
            """).getResultList();

        Map<Long, List<Map<String, Object>>> seatsMap = new HashMap<>();
        for (Object[] sr : seatRows) {
            Long bookingId = toLong(sr[0]);
            Map<String, Object> seat = new LinkedHashMap<>();
            seat.put("id",         toLong(sr[1]));
            seat.put("seatNumber", str(sr[2]));
            seatsMap.computeIfAbsent(bookingId, k -> new ArrayList<>()).add(seat);
        }

        // 3. Assemble response
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Long bookingId = toLong(r[0]);
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id",            bookingId);
            dto.put("status",        r[1] != null ? r[1].toString() : null);
            dto.put("totalPrice",    r[2]);
            dto.put("lockedAt",      r[3] != null ? r[3].toString() : null);
            dto.put("userId",        toLong(r[4]));
            dto.put("userName",      str(r[5]));
            dto.put("userEmail",     str(r[6]));
            dto.put("showId",        toLong(r[7]));
            dto.put("showDate",      str(r[8]));
            dto.put("showTime",      str(r[9]));
            dto.put("showPrice",     r[10]);
            dto.put("movieId",       toLong(r[11]));
            dto.put("movieTitle",    str(r[12]));
            dto.put("moviePosterUrl",str(r[13]));
            dto.put("venueId",       toLong(r[14]));
            dto.put("venueName",     str(r[15]));
            dto.put("venueCity",     str(r[16]));
            dto.put("venueArea",     str(r[17]));
            dto.put("seats",         seatsMap.getOrDefault(bookingId, List.of()));
            result.add(dto);
        }
        return result;
    }

    // ── DELETE /api/admin/bookings/{id}/cancel ────────────────
    @DeleteMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> forceCancel(@PathVariable Long id) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Booking not found: " + id));

        if (booking.getStatus() == BookingStatus.CANCELLED)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Already cancelled");
        if (booking.getStatus() == BookingStatus.EXPIRED)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Already expired");

        Long showId        = booking.getShow().getId();
        List<Long> seatIds = booking.getSeats().stream().map(Seat::getId).toList();
        seatLockService.unlockSeats(showId, seatIds);

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        return ResponseEntity.ok("Booking #" + id + " cancelled by admin");
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
}