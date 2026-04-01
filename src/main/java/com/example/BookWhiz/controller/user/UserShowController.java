package com.example.BookWhiz.controller.user;

import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * GET /api/user/shows?movieId=1              → all shows for that movie
 * GET /api/user/shows?movieId=1&city=Chennai → filtered by city
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY THIS EXISTS — THE CORE PROBLEM:
 *
 *   Old ShowController (JPA entity path) has two fatal bugs for EventDetails:
 *
 *   1. show.venue = null  (FetchType.LAZY — venue is never loaded in that path)
 *      → EventDetails groups all shows under "Unknown Theatre"
 *
 *   2. showTime serialised as [18, 30, 0]  (Jackson default for LocalTime)
 *      → Time buttons display "[object Object]" or "18,30,0"
 *
 *   This controller uses native SQL only — no JPA proxies, no lazy load,
 *   no Jackson LocalTime serialisation. Time is extracted directly as a
 *   trimmed "HH:mm" string. Venue fields are always populated.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * DEPLOYMENT (CRITICAL):
 *
 *   You MUST remove or rename any existing @GetMapping that lives
 *   under /api/user in any other controller (e.g. ShowController, LocationController).
 *   If two controllers map the same URL Spring Boot throws:
 *     "Ambiguous mapping. Cannot map 'userShowController'"
 *   and the application will NOT start.
 *
 *   The safest approach: search your project for
 *     @RequestMapping("/api/user/shows")  +  @GetMapping
 *   and delete that method.
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Response shape per show item:
 * {
 *   "id":             5,
 *   "showDate":       "2025-06-15",   ← always a plain string
 *   "showTime":       "18:30",        ← always "HH:mm", never [18,30,0]
 *   "price":          150.0,
 *   "totalSeats":     120,
 *   "availableSeats": 77,
 *   "venue": {
 *     "id":      3,
 *     "name":    "PVR Velachery",
 *     "city":    "Chennai",
 *     "area":    "Velachery",
 *     "address": "Phoenix Mall..."
 *   }
 * }
 */
@RestController
@RequestMapping("/api/user/shows")
@CrossOrigin(origins = "*")
public class UserShowController {

    private final EntityManager em;

    public UserShowController(EntityManager em) {
        this.em = em;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getShows(
            @RequestParam Long movieId,
            @RequestParam(required = false) String city
    ) {
        // ── Build SQL dynamically ─────────────────────────────
        StringBuilder sql = new StringBuilder("""
            SELECT
                s.id              AS showId,
                s.show_date       AS showDate,
                s.show_time       AS showTime,
                s.price           AS price,
                s.total_seats     AS totalSeats,
                v.id              AS venueId,
                v.name            AS venueName,
                v.city            AS venueCity,
                v.area            AS venueArea,
                v.address         AS venueAddress,
                (
                    SELECT COUNT(*)
                    FROM   bookings b
                    WHERE  b.show_id = s.id
                    AND    b.status  = 'CONFIRMED'
                ) AS seatsSold
            FROM  shows s
            LEFT  JOIN venue v ON v.id = s.venue_id
            WHERE s.movie_id = ?1
            AND   (
                s.show_date > CURDATE()
                OR (s.show_date = CURDATE() AND s.show_time >= CURTIME())
            )
            """);

        boolean filterCity = city != null && !city.isBlank();
        if (filterCity) {
            sql.append(" AND LOWER(v.city) = LOWER(?2) ");
        }

        sql.append(" ORDER BY s.show_date ASC, s.show_time ASC");

        var query = em.createNativeQuery(sql.toString());
        query.setParameter(1, movieId);
        if (filterCity) {
            query.setParameter(2, city.trim());
        }

        List<Object[]> rows = query.getResultList();

        // ── Build response ────────────────────────────────────
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] r : rows) {

            long totalSeats = num(r[4]);
            long seatsSold  = num(r[10]);
            long available  = Math.max(0, totalSeats - seatsSold);

            // showTime from MySQL TIME column arrives as "HH:mm:ss" — trim to "HH:mm"
            String rawTime  = r[2] != null ? r[2].toString() : "";
            String showTime = rawTime.length() >= 5 ? rawTime.substring(0, 5) : rawTime;

            Map<String, Object> show = new LinkedHashMap<>();
            show.put("id",             num(r[0]));
            show.put("showDate",       str(r[1]));   // "2025-06-15"
            show.put("showTime",       showTime);    // "18:30"
            show.put("price",          r[3] != null ? ((Number) r[3]).doubleValue() : null);
            show.put("totalSeats",     totalSeats);
            show.put("availableSeats", available);

            // Venue — only null if the show has no venue_id (shouldn't happen in practice)
            if (r[5] != null) {
                Map<String, Object> venue = new LinkedHashMap<>();
                venue.put("id",      num(r[5]));
                venue.put("name",    str(r[6]));
                venue.put("city",    str(r[7]));
                venue.put("area",    str(r[8]));
                venue.put("address", str(r[9]));
                show.put("venue", venue);
            } else {
                // Fallback so frontend never reads null.venue.name and crashes
                Map<String, Object> venue = new LinkedHashMap<>();
                venue.put("id",      null);
                venue.put("name",    "Unknown Theatre");
                venue.put("city",    "");
                venue.put("area",    "");
                venue.put("address", "");
                show.put("venue", venue);
            }

            result.add(show);
        }

        return result;
    }


    // ── GET /api/user/shows/{showId} ──────────────────────────
    // Returns full show details including venueId, movieTitle, showDate, showTime, price.
    // Used by SeatSelection.jsx to get the real venueId for the selected show.
    @GetMapping("/{showId}")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getShowById(@PathVariable Long showId) {

        List<Object[]> rows = em.createNativeQuery("""
            SELECT
                s.id            AS showId,
                s.show_date     AS showDate,
                s.show_time     AS showTime,
                s.price         AS price,
                s.total_seats   AS totalSeats,
                v.id            AS venueId,
                v.name          AS venueName,
                v.city          AS venueCity,
                v.area          AS venueArea,
                v.address       AS venueAddress,
                m.id            AS movieId,
                m.title         AS movieTitle,
                m.poster_url    AS posterUrl,
                m.language      AS language,
                m.format        AS format
            FROM  shows s
            LEFT  JOIN venue  v ON v.id = s.venue_id
            LEFT  JOIN movies m ON m.id = s.movie_id
            WHERE s.id = ?1
            """)
            .setParameter(1, showId)
            .getResultList();

        if (rows.isEmpty())
            return ResponseEntity.notFound().build();

        Object[] r = rows.get(0);

        String rawTime  = r[2] != null ? r[2].toString() : "";
        String showTime = rawTime.length() >= 5 ? rawTime.substring(0, 5) : rawTime;

        Map<String, Object> show = new LinkedHashMap<>();
        show.put("id",          num(r[0]));
        show.put("showDate",    str(r[1]));
        show.put("showTime",    showTime);
        show.put("price",       r[3] != null ? ((Number) r[3]).doubleValue() : null);
        show.put("totalSeats",  num(r[4]));
        show.put("venueId",     num(r[5]));   // ← the key field SeatSelection needs
        show.put("venueName",   str(r[6]));
        show.put("venueCity",   str(r[7]));
        show.put("venueArea",   str(r[8]));
        show.put("venueAddress",str(r[9]));
        show.put("movieId",     num(r[10]));
        show.put("movieTitle",  str(r[11]));
        show.put("posterUrl",   str(r[12]));
        show.put("language",    str(r[13]));
        show.put("format",      str(r[14]));

        return ResponseEntity.ok(show);
    }

    // ── Helpers ───────────────────────────────────────────────
    private static long   num(Object o) { return o == null ? 0L : ((Number) o).longValue(); }
    private static String str(Object o) { return o == null ? ""  : o.toString(); }
}