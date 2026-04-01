package com.example.BookWhiz.controller;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/theater-owner")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')")
@CrossOrigin(origins = "*")
public class TheaterOwnerController {

    private final EntityManager em;

    public TheaterOwnerController(EntityManager em) {
        this.em = em;
    }

    // ── 1. Summary ────────────────────────────────────────────
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(Authentication auth) {
        Long   venueId = getVenueId(auth);
        String vf      = venueFilter(venueId);

        long   totalShows   = queryLong("SELECT COUNT(*) FROM shows s WHERE 1=1 " + vf);
        double totalRevenue = queryDouble(
            "SELECT COALESCE(SUM(b.total_price),0) FROM bookings b " +
            "JOIN shows s ON s.id=b.show_id WHERE b.status='CONFIRMED' " + vf);
        long   totalBooks   = queryLong(
            "SELECT COUNT(*) FROM bookings b " +
            "JOIN shows s ON s.id=b.show_id WHERE b.status IN ('CONFIRMED','LOCKED') " + vf);
        long   totalMovies  = queryLong(
            "SELECT COUNT(DISTINCT s.movie_id) FROM shows s WHERE 1=1 " + vf);
        long   cancelled    = queryLong(
            "SELECT COUNT(*) FROM bookings b " +
            "JOIN shows s ON s.id=b.show_id WHERE b.status='CANCELLED' " + vf);
        long   occupied     = queryLong(
            "SELECT COUNT(bs.seat_id) FROM booking_seats bs " +
            "JOIN bookings b ON b.id=bs.booking_id " +
            "JOIN shows s ON s.id=b.show_id WHERE b.status='CONFIRMED' " + vf);
        long   capacity     = queryLong(
            "SELECT COALESCE(SUM(s.total_seats),0) FROM shows s WHERE 1=1 " + vf);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalShows",     totalShows);
        map.put("totalRevenue",   totalRevenue);
        map.put("totalBookings",  totalBooks);
        map.put("totalMovies",    totalMovies);
        map.put("cancelledCount", cancelled);
        map.put("occupancyRate",  capacity > 0
            ? Math.round((occupied * 100.0 / capacity) * 10) / 10.0 : 0);
        return ResponseEntity.ok(map);
    }

    // ── 2. Shows with per-show stats ──────────────────────────
    @GetMapping("/shows")
    public ResponseEntity<?> getShows(Authentication auth) {
        Long   venueId = getVenueId(auth);
        String vf      = venueFilter(venueId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT s.id, m.title AS movieTitle, m.genre, m.language, " +
            "  v.name AS venueName, v.city AS venueCity, " +
            "  s.show_date, s.show_time, s.price, s.total_seats, " +
            "  COUNT(DISTINCT b.id) AS totalBookings, " +
            "  SUM(CASE WHEN b.status='CONFIRMED' THEN 1 ELSE 0 END) AS confirmedBookings, " +
            "  SUM(CASE WHEN b.status='CANCELLED' THEN 1 ELSE 0 END) AS cancelledBookings, " +
            "  COALESCE(SUM(CASE WHEN b.status='CONFIRMED' THEN b.total_price ELSE 0 END),0) AS revenue, " +
            "  COUNT(DISTINCT CASE WHEN b.status='CONFIRMED' THEN bs.seat_id END) AS seatsBooked " +
            "FROM shows s " +
            "LEFT JOIN movies m ON m.id = s.movie_id " +
            "LEFT JOIN venue v ON v.id = s.venue_id " +
            "LEFT JOIN bookings b ON b.show_id = s.id " +
            "LEFT JOIN booking_seats bs ON bs.booking_id = b.id " +
            "WHERE 1=1 " + vf +
            " GROUP BY s.id, m.title, m.genre, m.language, v.name, v.city," +
            " s.show_date, s.show_time, s.price, s.total_seats" +
            " ORDER BY s.show_date DESC, s.show_time DESC"
        ).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",                toLong(r[0]));
            row.put("movieTitle",        r[1]);
            row.put("genre",             r[2]);
            row.put("language",          r[3]);
            row.put("venueName",         r[4]);
            row.put("venueCity",         r[5]);
            row.put("showDate",          r[6]);
            row.put("showTime",          r[7]);
            row.put("price",             toDouble(r[8]));
            row.put("totalSeats",        toLong(r[9]));
            row.put("totalBookings",     toLong(r[10]));
            row.put("confirmedBookings", toLong(r[11]));
            row.put("cancelledBookings", toLong(r[12]));
            row.put("revenue",           toDouble(r[13]));
            long seats = toLong(r[14]);
            long total = toLong(r[9]);
            row.put("seatsBooked", seats);
            row.put("occupancy",   total > 0
                ? Math.round((seats * 100.0 / total) * 10) / 10.0 : 0);
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    // ── 3. Single show bookings ───────────────────────────────
    @GetMapping("/shows/{id}/bookings")
    public ResponseEntity<?> getShowBookings(@PathVariable Long id) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT b.id, u.name, u.email, b.total_price, b.status, b.locked_at, " +
            "  COUNT(bs.seat_id) AS seatsCount " +
            "FROM bookings b " +
            "JOIN users u ON u.id = b.user_id " +
            "LEFT JOIN booking_seats bs ON bs.booking_id = b.id " +
            "WHERE b.show_id = ?1 " +
            "GROUP BY b.id, u.name, u.email, b.total_price, b.status, b.locked_at " +
            "ORDER BY b.locked_at DESC"
        ).setParameter(1, id).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bookingId",   toLong(r[0]));
            row.put("userName",    r[1]);
            row.put("userEmail",   r[2]);
            row.put("amount",      toDouble(r[3]));
            row.put("status",      r[4]);
            row.put("bookingTime", r[5]);
            row.put("seatsCount",  toLong(r[6]));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    // ── 4. Movies at venue ────────────────────────────────────
    @GetMapping("/movies")
    public ResponseEntity<?> getMovies(Authentication auth) {
        Long   venueId = getVenueId(auth);
        String vf      = venueFilter(venueId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT m.id, m.title, m.genre, m.language, m.format, m.duration, " +
            "  m.director, m.poster_url, m.release_date, " +
            "  COUNT(DISTINCT s.id) AS showCount, " +
            "  COALESCE(SUM(CASE WHEN b.status='CONFIRMED' THEN b.total_price ELSE 0 END),0) AS revenue, " +
            "  COUNT(DISTINCT CASE WHEN b.status='CONFIRMED' THEN b.id END) AS bookings " +
            "FROM movies m " +
            "JOIN shows s ON s.movie_id = m.id " +
            "LEFT JOIN bookings b ON b.show_id = s.id " +
            "WHERE 1=1 " + vf +
            " GROUP BY m.id, m.title, m.genre, m.language, m.format," +
            " m.duration, m.director, m.poster_url, m.release_date" +
            " ORDER BY revenue DESC"
        ).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",          toLong(r[0]));
            row.put("title",       r[1]);
            row.put("genre",       r[2]);
            row.put("language",    r[3]);
            row.put("format",      r[4]);
            row.put("duration",    r[5]);
            row.put("director",    r[6]);
            row.put("posterUrl",   r[7]);
            row.put("releaseDate", r[8]);
            row.put("showCount",   toLong(r[9]));
            row.put("revenue",     toDouble(r[10]));
            row.put("bookings",    toLong(r[11]));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    // ── 5. All bookings ───────────────────────────────────────
    @GetMapping("/bookings")
    public ResponseEntity<?> getAllBookings(Authentication auth) {
        Long   venueId = getVenueId(auth);
        String vf      = venueFilter(venueId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT b.id, u.name, u.email, " +
            "  m.title AS movieTitle, v.name AS venueName, " +
            "  s.show_date, s.show_time, b.total_price, b.status, b.locked_at, " +
            "  COUNT(bs.seat_id) AS seatsCount " +
            "FROM bookings b " +
            "JOIN users u ON u.id = b.user_id " +
            "JOIN shows s ON s.id = b.show_id " +
            "LEFT JOIN movies m ON m.id = s.movie_id " +
            "LEFT JOIN venue v ON v.id = s.venue_id " +
            "LEFT JOIN booking_seats bs ON bs.booking_id = b.id " +
            "WHERE 1=1 " + vf +
            " GROUP BY b.id, u.name, u.email, m.title, v.name," +
            " s.show_date, s.show_time, b.total_price, b.status, b.locked_at" +
            " ORDER BY b.locked_at DESC" +
            " LIMIT 500"
        ).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bookingId",   toLong(r[0]));
            row.put("userName",    r[1]);
            row.put("userEmail",   r[2]);
            row.put("movieTitle",  r[3]);
            row.put("venueName",   r[4]);
            row.put("showDate",    r[5]);
            row.put("showTime",    r[6]);
            row.put("amount",      toDouble(r[7]));
            row.put("status",      r[8]);
            row.put("bookingTime", r[9]);
            row.put("seatsCount",  toLong(r[10]));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    // ── 6. Today's stats ─────────────────────────────────────
    @GetMapping("/today")
    public ResponseEntity<?> getTodayStats(Authentication auth) {
        Long   venueId = getVenueId(auth);
        String vf      = venueFilter(venueId);
        String today   = LocalDate.now().toString();
        String yest    = LocalDate.now().minusDays(1).toString();

        double todayRevenue = queryDouble(
            "SELECT COALESCE(SUM(b.total_price),0) FROM bookings b " +
            "JOIN shows s ON s.id=b.show_id " +
            "WHERE b.status='CONFIRMED' AND DATE(b.locked_at)='" + today + "' " + vf);

        long todayBookings = queryLong(
            "SELECT COUNT(*) FROM bookings b JOIN shows s ON s.id=b.show_id " +
            "WHERE b.status IN ('CONFIRMED','LOCKED') AND DATE(b.locked_at)='" + today + "' " + vf);

        long todayCancelled = queryLong(
            "SELECT COUNT(*) FROM bookings b JOIN shows s ON s.id=b.show_id " +
            "WHERE b.status='CANCELLED' AND DATE(b.locked_at)='" + today + "' " + vf);

        long todayShows = queryLong(
            "SELECT COUNT(*) FROM shows s WHERE s.show_date='" + today + "'" +
            (venueId != null ? " AND s.venue_id=" + venueId : ""));

        double yestRevenue = queryDouble(
            "SELECT COALESCE(SUM(b.total_price),0) FROM bookings b " +
            "JOIN shows s ON s.id=b.show_id " +
            "WHERE b.status='CONFIRMED' AND DATE(b.locked_at)='" + yest + "' " + vf);

        double change = yestRevenue > 0
            ? Math.round(((todayRevenue - yestRevenue) / yestRevenue) * 1000) / 10.0
            : 0;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("todayRevenue",     todayRevenue);
        map.put("todayBookings",    todayBookings);
        map.put("todayCancelled",   todayCancelled);
        map.put("todayShows",       todayShows);
        map.put("yesterdayRevenue", yestRevenue);
        map.put("revenueChange",    change);
        map.put("estimatedProfit",  Math.round(todayRevenue * 0.7 * 100) / 100.0);
        map.put("estimatedCost",    Math.round(todayRevenue * 0.3 * 100) / 100.0);
        return ResponseEntity.ok(map);
    }

    // ── 7. Daily revenue chart ────────────────────────────────
    @GetMapping("/revenue/daily")
    public ResponseEntity<?> getDailyRevenue(Authentication auth) {
        Long   venueId = getVenueId(auth);
        String vf      = venueFilter(venueId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT DATE(b.locked_at) AS day, " +
            "  COALESCE(SUM(CASE WHEN b.status='CONFIRMED' THEN b.total_price ELSE 0 END),0) AS revenue, " +
            "  COUNT(CASE WHEN b.status='CONFIRMED' THEN 1 END) AS bookings, " +
            "  COUNT(CASE WHEN b.status='CANCELLED' THEN 1 END) AS cancelled " +
            "FROM bookings b JOIN shows s ON s.id=b.show_id " +
            "WHERE b.locked_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " + vf +
            " GROUP BY DATE(b.locked_at)" +
            " ORDER BY day ASC"
        ).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date",      r[0]);
            row.put("revenue",   toDouble(r[1]));
            row.put("bookings",  toLong(r[2]));
            row.put("cancelled", toLong(r[3]));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    // ── 8. CSV export ─────────────────────────────────────────
    @GetMapping("/export")
    public void exportCSV(Authentication auth, HttpServletResponse response) throws Exception {
        Long   venueId = getVenueId(auth);
        String vf      = venueFilter(venueId);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
            "attachment; filename=\"booking-history-" + LocalDate.now() + ".csv\"");

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT b.id, u.name, u.email, m.title, v.name, v.city, " +
            "  s.show_date, s.show_time, s.price, " +
            "  COUNT(bs.seat_id) AS seatsCount, b.total_price, b.status, b.locked_at " +
            "FROM bookings b " +
            "JOIN users u ON u.id = b.user_id " +
            "JOIN shows s ON s.id = b.show_id " +
            "LEFT JOIN movies m ON m.id = s.movie_id " +
            "LEFT JOIN venue v ON v.id = s.venue_id " +
            "LEFT JOIN booking_seats bs ON bs.booking_id = b.id " +
            "WHERE 1=1 " + vf +
            " GROUP BY b.id, u.name, u.email, m.title, v.name, v.city," +
            " s.show_date, s.show_time, s.price, b.total_price, b.status, b.locked_at" +
            " ORDER BY b.locked_at DESC"
        ).getResultList();

        PrintWriter pw = response.getWriter();
        pw.println("Booking ID,Customer Name,Email,Movie,Venue,City," +
                   "Show Date,Show Time,Price/Seat,Seats,Total Amount,Status,Booking Time");
        for (Object[] r : rows) {
            pw.println(String.join(",",
                safe(r[0]),  safe(r[1]),  safe(r[2]),  safe(r[3]),
                safe(r[4]),  safe(r[5]),  safe(r[6]),  safe(r[7]),
                safe(r[8]),  safe(r[9]),  safe(r[10]), safe(r[11]), safe(r[12])
            ));
        }
        pw.flush();
    }

    // ── Helpers ───────────────────────────────────────────────

    /** Always returns a string with a LEADING space so "WHERE 1=1 " + vf is safe */
    private String venueFilter(Long venueId) {
        return venueId != null ? " AND s.venue_id=" + venueId : "";
    }

    private Long getVenueId(Authentication auth) {
        return null; // extend later: User → managedVenueId
    }

    private long queryLong(String sql) {
        Object r = em.createNativeQuery(sql).getSingleResult();
        return r == null ? 0L : ((Number) r).longValue();
    }

    private double queryDouble(String sql) {
        Object r = em.createNativeQuery(sql).getSingleResult();
        return r == null ? 0.0 : ((Number) r).doubleValue();
    }

    private long   toLong(Object o)   { return o == null ? 0L   : ((Number) o).longValue();   }
    private double toDouble(Object o) { return o == null ? 0.0  : ((Number) o).doubleValue();  }

    private String safe(Object o) {
        if (o == null) return "\"\"";
        return "\"" + o.toString().replace("\"", "").replace(",", "") + "\"";
    }
}