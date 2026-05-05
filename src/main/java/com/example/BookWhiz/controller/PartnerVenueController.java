package com.example.BookWhiz.controller;

import com.example.BookWhiz.dto.request.ShowRequest;
import com.example.BookWhiz.model.venue.Venue;
import com.example.BookWhiz.service.ShowService;
import com.example.BookWhiz.service.VenueService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;

/**
 * Partner Portal — authenticated MANAGER / ADMIN / SUPER_ADMIN.
 *
 * All venue/show operations are scoped to venues owned by the logged-in user
 * (matched by owner_email = auth.getName()).
 *
 * Endpoints:
 *   VENUE:     GET/POST /api/partner-portal/venues
 *              PUT/DELETE /api/partner-portal/venues/{id}
 *
 *   SHOWS:     GET/POST /api/partner-portal/shows
 *              PUT/DELETE /api/partner-portal/shows/{id}
 *              GET /api/partner-portal/shows/{id}/bookings
 *
 *   MOVIES:    GET /api/partner-portal/movies   (all available movies for dropdown)
 *
 *   ANALYTICS: GET /api/partner-portal/analytics
 *              GET /api/partner-portal/today
 *              GET /api/partner-portal/revenue/daily
 *
 *   BOOKINGS:  GET /api/partner-portal/bookings
 *
 *   EXPORT:    GET /api/partner-portal/export
 *
 *   SCAN:      POST /api/partner-portal/scan
 */
@RestController
@RequestMapping("/api/partner-portal")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')")
@CrossOrigin(origins = "*")
public class PartnerVenueController {

    private final VenueService venueService;
    private final ShowService  showService;
    private final EntityManager em;

    public PartnerVenueController(VenueService venueService,
                                  ShowService showService,
                                  EntityManager em) {
        this.venueService = venueService;
        this.showService  = showService;
        this.em           = em;
    }

    // ════════════════════════════════════════════════
    //  VENUE CRUD
    // ════════════════════════════════════════════════

    @GetMapping("/venues")
    public ResponseEntity<?> getMyVenues(Authentication auth) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT id, name, city, area, address, total_seats, screen_name, amenities " +
            "FROM venue WHERE owner_email = ?1 ORDER BY id DESC"
        ).setParameter(1, auth.getName()).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id",         toLong(r[0]));
            v.put("name",       str(r[1]));
            v.put("city",       str(r[2]));
            v.put("area",       str(r[3]));
            v.put("address",    str(r[4]));
            v.put("totalSeats", toLong(r[5]));
            v.put("screenName", str(r[6]));
            v.put("amenities",  str(r[7]));
            result.add(v);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/venues")
    public ResponseEntity<?> createVenue(@RequestBody Venue venue, Authentication auth) {
        if (blank(venue.getName()))    return bad("Theatre name is required");
        if (blank(venue.getCity()))    return bad("City is required");
        if (blank(venue.getAddress())) return bad("Address is required");
        if (venue.getTotalSeats() == null || venue.getTotalSeats() < 1) return bad("Total seats must be > 0");

        venue.setOwnerEmail(auth.getName());
        return ResponseEntity.ok(venueService.createVenue(venue));
    }

    @PutMapping("/venues/{id}")
    public ResponseEntity<?> updateVenue(@PathVariable Long id,
                                         @RequestBody Venue venue,
                                         Authentication auth) {
        if (!isMyVenue(id, auth.getName()))
            return ResponseEntity.status(403).body(Map.of("error", "Not your venue"));
        venue.setOwnerEmail(auth.getName());
        return venueService.updateVenue(id, venue)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/venues/{id}")
    public ResponseEntity<?> deleteVenue(@PathVariable Long id, Authentication auth) {
        if (!isMyVenue(id, auth.getName()))
            return ResponseEntity.status(403).body(Map.of("error", "Not your venue"));
        venueService.deleteVenue(id);
        return ResponseEntity.ok(Map.of("message", "Venue deleted"));
    }

    // ════════════════════════════════════════════════
    //  SHOWS CRUD
    // ════════════════════════════════════════════════

    @GetMapping("/shows")
    public ResponseEntity<?> getMyShows(Authentication auth) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT s.id, m.id, m.title, m.genre, m.language, m.format, m.poster_url, " +
            "  v.id, v.name, v.city, v.screen_name, " +
            "  s.show_date, s.show_time, s.price, s.total_seats, " +
            "  (SELECT COUNT(*) FROM bookings b WHERE b.show_id=s.id AND b.status='CONFIRMED') AS confirmedCount " +
            "FROM shows s " +
            "LEFT JOIN movies m ON m.id = s.movie_id " +
            "LEFT JOIN venue  v ON v.id = s.venue_id " +
            "WHERE v.owner_email = ?1 " +
            "ORDER BY s.show_date DESC, s.show_time DESC"
        ).setParameter(1, auth.getName()).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",             toLong(r[0]));
            row.put("movieId",        toLong(r[1]));
            row.put("movieTitle",     str(r[2]));
            row.put("genre",          str(r[3]));
            row.put("language",       str(r[4]));
            row.put("format",         str(r[5]));
            row.put("posterUrl",      str(r[6]));
            row.put("venueId",        toLong(r[7]));
            row.put("venueName",      str(r[8]));
            row.put("city",           str(r[9]));
            row.put("screenName",     str(r[10]));
            row.put("showDate",       str(r[11]));
            row.put("showTime",       str(r[12]));
            row.put("price",          toDouble(r[13]));
            row.put("totalSeats",     toLong(r[14]));
            row.put("confirmedCount", toLong(r[15]));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/shows")
    public ResponseEntity<?> createShow(@RequestBody ShowRequest req, Authentication auth) {
        if (req.getVenueId() == null) return bad("Venue is required");
        if (!isMyVenue(req.getVenueId(), auth.getName()))
            return ResponseEntity.status(403)
                .body(Map.of("error", "You can only add shows to your own venues"));
        if (req.getMovieId() == null)  return bad("Movie is required");
        if (blank(req.getShowDate()))  return bad("Show date is required");
        if (blank(req.getShowTime()))  return bad("Show time is required");
        if (req.getPrice() == null)    return bad("Price is required");

        return ResponseEntity.ok(showService.createShow(req));
    }

    @PutMapping("/shows/{id}")
    @Transactional
    public ResponseEntity<?> updateShow(@PathVariable Long id,
                                        @RequestBody ShowRequest req,
                                        Authentication auth) {
        if (!isMyShow(id, auth.getName()))
            return ResponseEntity.status(403).body(Map.of("error", "Not your show"));
        return showService.updateShow(id, req)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/shows/{id}")
    public ResponseEntity<?> deleteShow(@PathVariable Long id, Authentication auth) {
        if (!isMyShow(id, auth.getName()))
            return ResponseEntity.status(403).body(Map.of("error", "Not your show"));
        showService.deleteShow(id);
        return ResponseEntity.ok(Map.of("message", "Show deleted"));
    }

    @GetMapping("/shows/{id}/bookings")
    public ResponseEntity<?> getShowBookings(@PathVariable Long id, Authentication auth) {
        if (!isMyShow(id, auth.getName()))
            return ResponseEntity.status(403).body(Map.of("error", "Not your show"));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT b.id, u.name, u.email, b.total_price, b.status, b.locked_at, " +
            "  COUNT(bs.seat_id) " +
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
            row.put("userName",    str(r[1]));
            row.put("userEmail",   str(r[2]));
            row.put("amount",      toDouble(r[3]));
            row.put("status",      str(r[4]));
            row.put("bookingTime", str(r[5]));
            row.put("seats",       toLong(r[6]));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    // ════════════════════════════════════════════════
    //  MOVIES (for dropdown when creating shows)
    // ════════════════════════════════════════════════

    @GetMapping("/movies")
    public ResponseEntity<?> getAvailableMovies() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT id, title, genre, language, format, duration, poster_url FROM movies ORDER BY title ASC"
        ).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        toLong(r[0]));
            m.put("title",     str(r[1]));
            m.put("genre",     str(r[2]));
            m.put("language",  str(r[3]));
            m.put("format",    str(r[4]));
            m.put("duration",  str(r[5]));
            m.put("posterUrl", str(r[6]));
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // ════════════════════════════════════════════════
    //  ANALYTICS
    // ════════════════════════════════════════════════

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(Authentication auth) {
        String e = esc(auth.getName());

        double totalRevenue  = qDouble("SELECT COALESCE(SUM(b.total_price),0) FROM bookings b JOIN shows s ON s.id=b.show_id JOIN venue v ON v.id=s.venue_id WHERE b.status='CONFIRMED' AND v.owner_email='" + e + "'");
        long   totalBookings = qLong  ("SELECT COUNT(*) FROM bookings b JOIN shows s ON s.id=b.show_id JOIN venue v ON v.id=s.venue_id WHERE b.status='CONFIRMED' AND v.owner_email='" + e + "'");
        long   totalShows    = qLong  ("SELECT COUNT(*) FROM shows s JOIN venue v ON v.id=s.venue_id WHERE v.owner_email='" + e + "'");
        long   totalVenues   = qLong  ("SELECT COUNT(*) FROM venue WHERE owner_email='" + e + "'");
        long   cancelled     = qLong  ("SELECT COUNT(*) FROM bookings b JOIN shows s ON s.id=b.show_id JOIN venue v ON v.id=s.venue_id WHERE b.status='CANCELLED' AND v.owner_email='" + e + "'");
        long   totalSeats    = qLong  ("SELECT COALESCE(SUM(v.total_seats),0) FROM venue v WHERE v.owner_email='" + e + "'");
        long   seatsSold     = qLong  ("SELECT COUNT(bs.seat_id) FROM booking_seats bs JOIN bookings b ON b.id=bs.booking_id JOIN shows s ON s.id=b.show_id JOIN venue v ON v.id=s.venue_id WHERE b.status='CONFIRMED' AND v.owner_email='" + e + "'");

        // Daily revenue last 30 days
        @SuppressWarnings("unchecked")
        List<Object[]> daily = em.createNativeQuery(
            "SELECT DATE(b.locked_at), " +
            "  COALESCE(SUM(CASE WHEN b.status='CONFIRMED' THEN b.total_price ELSE 0 END),0), " +
            "  COUNT(CASE WHEN b.status='CONFIRMED' THEN 1 END), " +
            "  COUNT(CASE WHEN b.status='CANCELLED' THEN 1 END) " +
            "FROM bookings b JOIN shows s ON s.id=b.show_id JOIN venue v ON v.id=s.venue_id " +
            "WHERE v.owner_email='" + e + "' AND b.locked_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(b.locked_at) ORDER BY DATE(b.locked_at) ASC"
        ).getResultList();

        List<Map<String, Object>> dailyList = new ArrayList<>();
        for (Object[] r : daily) {
            dailyList.add(Map.of(
                "date",      str(r[0]),
                "revenue",   toDouble(r[1]),
                "bookings",  toLong(r[2]),
                "cancelled", toLong(r[3])
            ));
        }

        // Top 5 movies by revenue
        @SuppressWarnings("unchecked")
        List<Object[]> top = em.createNativeQuery(
            "SELECT m.title, COALESCE(SUM(b.total_price),0), COUNT(DISTINCT b.id) " +
            "FROM bookings b JOIN shows s ON s.id=b.show_id JOIN movies m ON m.id=s.movie_id JOIN venue v ON v.id=s.venue_id " +
            "WHERE b.status='CONFIRMED' AND v.owner_email='" + e + "' " +
            "GROUP BY m.id, m.title ORDER BY 2 DESC LIMIT 5"
        ).getResultList();

        List<Map<String, Object>> topMovies = new ArrayList<>();
        for (Object[] r : top) {
            topMovies.add(Map.of("title", str(r[0]), "revenue", toDouble(r[1]), "bookings", toLong(r[2])));
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalRevenue",    totalRevenue);
        map.put("totalBookings",   totalBookings);
        map.put("totalShows",      totalShows);
        map.put("totalVenues",     totalVenues);
        map.put("cancelledCount",  cancelled);
        map.put("totalSeats",      totalSeats);
        map.put("seatsSold",       seatsSold);
        map.put("occupancyRate",   totalSeats > 0 ? Math.round(seatsSold * 100.0 / totalSeats * 10) / 10.0 : 0);
        map.put("estimatedProfit", Math.round(totalRevenue * 0.70 * 100) / 100.0);
        map.put("estimatedCost",   Math.round(totalRevenue * 0.30 * 100) / 100.0);
        map.put("dailyRevenue",    dailyList);
        map.put("topMovies",       topMovies);
        return ResponseEntity.ok(map);
    }

    @GetMapping("/today")
    public ResponseEntity<?> getToday(Authentication auth) {
        String e     = esc(auth.getName());
        String today = LocalDate.now().toString();
        String yest  = LocalDate.now().minusDays(1).toString();

        double todayRev      = qDouble("SELECT COALESCE(SUM(b.total_price),0) FROM bookings b JOIN shows s ON s.id=b.show_id JOIN venue v ON v.id=s.venue_id WHERE b.status='CONFIRMED' AND DATE(b.locked_at)='" + today + "' AND v.owner_email='" + e + "'");
        long   todayBooks    = qLong  ("SELECT COUNT(*) FROM bookings b JOIN shows s ON s.id=b.show_id JOIN venue v ON v.id=s.venue_id WHERE b.status='CONFIRMED' AND DATE(b.locked_at)='" + today + "' AND v.owner_email='" + e + "'");
        long   todayCancel   = qLong  ("SELECT COUNT(*) FROM bookings b JOIN shows s ON s.id=b.show_id JOIN venue v ON v.id=s.venue_id WHERE b.status='CANCELLED' AND DATE(b.locked_at)='" + today + "' AND v.owner_email='" + e + "'");
        double yestRev       = qDouble("SELECT COALESCE(SUM(b.total_price),0) FROM bookings b JOIN shows s ON s.id=b.show_id JOIN venue v ON v.id=s.venue_id WHERE b.status='CONFIRMED' AND DATE(b.locked_at)='" + yest + "' AND v.owner_email='" + e + "'");

        // Today's bookings list
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT b.id, u.name, u.email, m.title, v.name, s.show_date, s.show_time, b.total_price, b.status, COUNT(bs.seat_id) " +
            "FROM bookings b JOIN users u ON u.id=b.user_id JOIN shows s ON s.id=b.show_id " +
            "LEFT JOIN movies m ON m.id=s.movie_id LEFT JOIN venue v ON v.id=s.venue_id " +
            "LEFT JOIN booking_seats bs ON bs.booking_id=b.id " +
            "WHERE v.owner_email='" + e + "' AND DATE(b.locked_at)='" + today + "' " +
            "GROUP BY b.id,u.name,u.email,m.title,v.name,s.show_date,s.show_time,b.total_price,b.status " +
            "ORDER BY b.locked_at DESC"
        ).getResultList();

        List<Map<String, Object>> bookings = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bookingId",  toLong(r[0]));
            row.put("userName",   str(r[1]));
            row.put("userEmail",  str(r[2]));
            row.put("movie",      str(r[3]));
            row.put("venue",      str(r[4]));
            row.put("showDate",   str(r[5]));
            row.put("showTime",   str(r[6]));
            row.put("amount",     toDouble(r[7]));
            row.put("status",     str(r[8]));
            row.put("seats",      toLong(r[9]));
            bookings.add(row);
        }

        double change = yestRev > 0 ? Math.round(((todayRev - yestRev) / yestRev) * 1000) / 10.0 : 0;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("todayRevenue",    todayRev);
        map.put("todayBookings",   todayBooks);
        map.put("todayCancelled",  todayCancel);
        map.put("yesterdayRevenue",yestRev);
        map.put("revenueChange",   change);
        map.put("estimatedProfit", Math.round(todayRev * 0.7 * 100) / 100.0);
        map.put("todayBookingsList", bookings);
        return ResponseEntity.ok(map);
    }

    // ════════════════════════════════════════════════
    //  ALL BOOKINGS
    // ════════════════════════════════════════════════

    @GetMapping("/bookings")
    public ResponseEntity<?> getBookings(Authentication auth) {
        String e = esc(auth.getName());
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT b.id, u.name, u.email, m.title, v.name, s.show_date, s.show_time, " +
            "  b.total_price, b.status, b.locked_at, COUNT(bs.seat_id) " +
            "FROM bookings b JOIN users u ON u.id=b.user_id " +
            "JOIN shows s ON s.id=b.show_id " +
            "LEFT JOIN movies m ON m.id=s.movie_id LEFT JOIN venue v ON v.id=s.venue_id " +
            "LEFT JOIN booking_seats bs ON bs.booking_id=b.id " +
            "WHERE v.owner_email='" + e + "' " +
            "GROUP BY b.id,u.name,u.email,m.title,v.name,s.show_date,s.show_time,b.total_price,b.status,b.locked_at " +
            "ORDER BY b.locked_at DESC LIMIT 500"
        ).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bookingId",   toLong(r[0]));
            row.put("userName",    str(r[1]));
            row.put("userEmail",   str(r[2]));
            row.put("movieTitle",  str(r[3]));
            row.put("venueName",   str(r[4]));
            row.put("showDate",    str(r[5]));
            row.put("showTime",    str(r[6]));
            row.put("amount",      toDouble(r[7]));
            row.put("status",      str(r[8]));
            row.put("bookingTime", str(r[9]));
            row.put("seats",       toLong(r[10]));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    // ════════════════════════════════════════════════
    //  CSV EXPORT
    // ════════════════════════════════════════════════

    @GetMapping("/export")
    public void exportCSV(Authentication auth, HttpServletResponse res) throws Exception {
        String e = esc(auth.getName());
        res.setContentType("text/csv");
        res.setHeader("Content-Disposition",
            "attachment; filename=\"bookings-" + LocalDate.now() + ".csv\"");

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT b.id, u.name, u.email, m.title, v.name, v.city, " +
            "  s.show_date, s.show_time, s.price, COUNT(bs.seat_id), b.total_price, b.status, b.locked_at " +
            "FROM bookings b JOIN users u ON u.id=b.user_id " +
            "JOIN shows s ON s.id=b.show_id " +
            "LEFT JOIN movies m ON m.id=s.movie_id LEFT JOIN venue v ON v.id=s.venue_id " +
            "LEFT JOIN booking_seats bs ON bs.booking_id=b.id " +
            "WHERE v.owner_email='" + e + "' " +
            "GROUP BY b.id,u.name,u.email,m.title,v.name,v.city,s.show_date,s.show_time,s.price,b.total_price,b.status,b.locked_at " +
            "ORDER BY b.locked_at DESC"
        ).getResultList();

        PrintWriter pw = res.getWriter();
        pw.println("Booking ID,Customer,Email,Movie,Theatre,City,Date,Time,Price/Seat,Seats,Total,Status,Booked At");
        for (Object[] r : rows) {
            pw.println(csv(r[0],r[1],r[2],r[3],r[4],r[5],r[6],r[7],r[8],r[9],r[10],r[11],r[12]));
        }
        pw.flush();
    }

    // ════════════════════════════════════════════════
    //  TICKET SCAN
    // ════════════════════════════════════════════════

    @PostMapping("/scan")
    public ResponseEntity<?> scanTicket(@RequestBody Map<String, Object> body, Authentication auth) {
        Object rawId = body.get("bookingId");
        if (rawId == null)
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "bookingId required"));

        long bookingId;
        try { bookingId = Long.parseLong(rawId.toString()); }
        catch (NumberFormatException ex) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Invalid booking ID"));
        }

        String email = esc(auth.getName());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT b.id, u.name, u.email, b.status, b.total_price, b.locked_at, " +
            "  m.title, v.name, s.show_date, s.show_time, COUNT(bs.seat_id) " +
            "FROM bookings b JOIN users u ON u.id=b.user_id " +
            "JOIN shows s ON s.id=b.show_id " +
            "LEFT JOIN movies m ON m.id=s.movie_id LEFT JOIN venue v ON v.id=s.venue_id " +
            "LEFT JOIN booking_seats bs ON bs.booking_id=b.id " +
            "WHERE b.id=?1 AND v.owner_email=?2 " +
            "GROUP BY b.id,u.name,u.email,b.status,b.total_price,b.locked_at,m.title,v.name,s.show_date,s.show_time"
        ).setParameter(1, bookingId).setParameter(2, auth.getName()).getResultList();

        if (rows.isEmpty())
            return ResponseEntity.status(404).body(Map.of(
                "valid", false,
                "error", "Booking #" + bookingId + " not found or does not belong to your venue"
            ));

        Object[] r      = rows.get(0);
        String   status = str(r[3]);
        boolean  valid  = "CONFIRMED".equals(status);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid",        valid);
        result.put("bookingId",    toLong(r[0]));
        result.put("customerName", str(r[1]));
        result.put("email",        str(r[2]));
        result.put("status",       status);
        result.put("amount",       toDouble(r[4]));
        result.put("bookedAt",     str(r[5]));
        result.put("movie",        str(r[6]));
        result.put("venue",        str(r[7]));
        result.put("showDate",     str(r[8]));
        result.put("showTime",     str(r[9]));
        result.put("seats",        toLong(r[10]));
        result.put("message",      valid ? "✅ Valid ticket — entry permitted" : "❌ Ticket is " + status.toLowerCase());
        return ResponseEntity.ok(result);
    }

    // ════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════

    private boolean isMyVenue(Long venueId, String email) {
        if (venueId == null) return false;
        Object r = em.createNativeQuery(
            "SELECT COUNT(*) FROM venue WHERE id=?1 AND owner_email=?2"
        ).setParameter(1, venueId).setParameter(2, email).getSingleResult();
        return ((Number) r).longValue() > 0;
    }

    private boolean isMyShow(Long showId, String email) {
        if (showId == null) return false;
        Object r = em.createNativeQuery(
            "SELECT COUNT(*) FROM shows s JOIN venue v ON v.id=s.venue_id " +
            "WHERE s.id=?1 AND v.owner_email=?2"
        ).setParameter(1, showId).setParameter(2, email).getSingleResult();
        return ((Number) r).longValue() > 0;
    }

    private long     qLong(String sql)    { Object r = em.createNativeQuery(sql).getSingleResult(); return r == null ? 0L : ((Number) r).longValue(); }
    private double   qDouble(String sql)  { Object r = em.createNativeQuery(sql).getSingleResult(); return r == null ? 0.0 : ((Number) r).doubleValue(); }
    private long     toLong(Object o)     { return o == null ? 0L   : ((Number) o).longValue(); }
    private double   toDouble(Object o)   { return o == null ? 0.0  : ((Number) o).doubleValue(); }
    private String   str(Object o)        { return o == null ? ""   : o.toString(); }
    private boolean  blank(String s)      { return s == null || s.isBlank(); }
    private ResponseEntity<?> bad(String msg) { return ResponseEntity.badRequest().body(Map.of("error", msg)); }

    /** Escape single quotes to prevent SQL injection in string-interpolated queries */
    private String esc(String s) { return s == null ? "" : s.replace("'", "''"); }

    private String csv(Object... vals) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            if (i > 0) sb.append(",");
            String v = vals[i] == null ? "" : vals[i].toString().replace("\"","").replace(",","");
            sb.append("\"").append(v).append("\"");
        }
        return sb.toString();
    }
}