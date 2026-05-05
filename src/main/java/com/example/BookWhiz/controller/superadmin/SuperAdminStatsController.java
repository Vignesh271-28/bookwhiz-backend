package com.example.BookWhiz.controller.superadmin;

import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * GET /api/superadmin/stats
 * GET /api/superadmin/revenue
 *
 * Revenue is calculated directly from bookings.total_price (CONFIRMED bookings).
 * Does NOT depend on a payments table — no payments table = revenue still works.
 *
 * WHY REVENUE WAS ALWAYS 0:
 *   The old AnalyticsServiceImpl.getPlatformStats() read from paymentRepository.
 *   But the booking flow (lockSeats → confirm) never inserts a Payment row.
 *   And bookings.total_price was never set during lockSeats (price × seats).
 *
 * THIS CONTROLLER FIXES BOTH:
 *   1. Revenue = SUM(total_price) WHERE status = 'CONFIRMED'
 *   2. Monthly breakdown = GROUP BY month from confirmed_at
 */
@RestController
@RequestMapping("/api/superadmin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
@CrossOrigin
public class SuperAdminStatsController {

    private final EntityManager em;

    public SuperAdminStatsController(EntityManager em) {
        this.em = em;
    }

    // ── GET /api/superadmin/stats ─────────────────────────────
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getStats() {

        // User counts by role
        // Strip ROLE_ prefix so keys are always "USER","ADMIN","MANAGER","SUPER_ADMIN"
        // regardless of whether the roles table stores "USER" or "ROLE_USER"
        List<Object[]> roleRows = em.createNativeQuery("""
            SELECT REPLACE(UPPER(r.name), 'ROLE_', '') AS roleName,
                   COUNT(ur.user_id) AS cnt
            FROM roles r
            LEFT JOIN user_roles ur ON ur.role_id = r.id
            GROUP BY r.name
            """).getResultList();

        Map<String, Long> roleCounts = new HashMap<>();
        for (Object[] r : roleRows) {
            roleCounts.put(str(r[0]), num(r[1]));
        }

        // Booking counts by status
        List<Object[]> bookingRows = em.createNativeQuery("""
            SELECT status, COUNT(*) FROM bookings GROUP BY status
            """).getResultList();

        Map<String, Long> bookingCounts = new HashMap<>();
        for (Object[] r : bookingRows) {
            bookingCounts.put(str(r[0]), num(r[1]));
        }

        // Revenue from confirmed bookings (total_price column)
        Object revenueObj = em.createNativeQuery("""
            SELECT COALESCE(SUM(total_price), 0)
            FROM bookings
            WHERE status = 'CONFIRMED'
            """).getSingleResult();
        double totalRevenue = revenueObj != null ? ((Number) revenueObj).doubleValue() : 0.0;

        // Venue + movie counts
        long totalVenues = num(em.createNativeQuery("SELECT COUNT(*) FROM venue").getSingleResult());
        long totalMovies = num(em.createNativeQuery("SELECT COUNT(*) FROM movies").getSingleResult());
        long totalShows  = num(em.createNativeQuery("SELECT COUNT(*) FROM shows").getSingleResult());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers",        roleCounts.getOrDefault("USER",        0L));
        stats.put("totalManagers",     roleCounts.getOrDefault("MANAGER",     0L));
        stats.put("totalAdmins",       roleCounts.getOrDefault("ADMIN",       0L));
        stats.put("totalSuperAdmins",  roleCounts.getOrDefault("SUPER_ADMIN", 0L));

        // Custom role breakdown
        List<Object[]> customRoleRows = em.createNativeQuery(
            "SELECT cr.name, cr.display_name, cr.color, cr.icon, COUNT(ucr.user_id) AS cnt " +
            "FROM custom_roles cr " +
            "LEFT JOIN user_custom_roles ucr ON ucr.custom_role_id = cr.id " +
            "GROUP BY cr.id, cr.name, cr.display_name, cr.color, cr.icon"
        ).getResultList();
        List<Map<String, Object>> customRoleStats = new ArrayList<>();
        for (Object[] r : customRoleRows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name",        str(r[0]));
            row.put("displayName", str(r[1]));
            row.put("color",       str(r[2]));
            row.put("icon",        str(r[3]));
            row.put("count",       num(r[4]));
            customRoleStats.add(row);
        }
        stats.put("customRoles", customRoleStats);
        stats.put("totalRevenue",      totalRevenue);
        stats.put("totalBookings",     bookingCounts.values().stream().mapToLong(Long::longValue).sum());
        stats.put("confirmedBookings", bookingCounts.getOrDefault("CONFIRMED", 0L));
        stats.put("cancelledBookings", bookingCounts.getOrDefault("CANCELLED", 0L));
        stats.put("pendingBookings",   bookingCounts.getOrDefault("LOCKED",    0L));
        stats.put("activeEvents",      totalShows);
        stats.put("totalVenues",       totalVenues);
        stats.put("totalMovies",       totalMovies);

        return ResponseEntity.ok(stats);
    }

    // ── GET /api/superadmin/revenue?period=daily|weekly|monthly ──
    // Returns revenue grouped by the chosen period (default: monthly)
    @GetMapping("/revenue")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getRevenue(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "monthly") String period) {

        // Build query based on requested period
        String sql;
        int limit;
        if ("daily".equalsIgnoreCase(period)) {
            sql = """
                SELECT
                    DATE_FORMAT(DATE(COALESCE(confirmed_at, locked_at)), '%d %b') AS label,
                    YEAR(DATE(COALESCE(confirmed_at, locked_at)))     AS yr,
                    DAYOFYEAR(DATE(COALESCE(confirmed_at, locked_at))) AS mo,
                    COALESCE(SUM(total_price), 0)                     AS revenue,
                    COUNT(*)                                           AS bookings
                FROM bookings
                WHERE status = 'CONFIRMED'
                  AND COALESCE(confirmed_at, locked_at) >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
                GROUP BY DATE(COALESCE(confirmed_at, locked_at)),
                         YEAR(DATE(COALESCE(confirmed_at, locked_at))),
                         DAYOFYEAR(DATE(COALESCE(confirmed_at, locked_at)))
                ORDER BY yr ASC, mo ASC
                """;
            limit = 30;
        } else if ("weekly".equalsIgnoreCase(period)) {
            sql = """
                SELECT
                    CONCAT('W', LPAD(WEEK(COALESCE(confirmed_at, locked_at), 1), 2, '0'),
                           ' ', YEAR(COALESCE(confirmed_at, locked_at))) AS label,
                    YEAR(COALESCE(confirmed_at, locked_at))              AS yr,
                    WEEK(COALESCE(confirmed_at, locked_at), 1)           AS mo,
                    COALESCE(SUM(total_price), 0)                        AS revenue,
                    COUNT(*)                                              AS bookings
                FROM bookings
                WHERE status = 'CONFIRMED'
                  AND COALESCE(confirmed_at, locked_at) >= DATE_SUB(CURDATE(), INTERVAL 12 WEEK)
                GROUP BY yr, mo
                ORDER BY yr ASC, mo ASC
                """;;
            limit = 12;
        } else {
            sql = """
                SELECT
                    DATE_FORMAT(COALESCE(confirmed_at, locked_at), '%b %Y') AS label,
                    YEAR(COALESCE(confirmed_at, locked_at))  AS yr,
                    MONTH(COALESCE(confirmed_at, locked_at)) AS mo,
                    COALESCE(SUM(total_price), 0) AS revenue,
                    COUNT(*) AS bookings
                FROM bookings
                WHERE status = 'CONFIRMED'
                  AND COALESCE(confirmed_at, locked_at) IS NOT NULL
                GROUP BY yr, mo, label
                ORDER BY yr ASC, mo ASC
                """;
            limit = 12;
        }

        // Monthly confirmed revenue from bookings
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        // If no confirmed bookings yet, return last 6 months with zeros
        if (rows.isEmpty()) {
            List<Map<String, Object>> empty = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            for (int i = 5; i >= 0; i--) {
                cal.setTime(new Date());
                cal.add(Calendar.MONTH, -i);
                String monthName = new java.text.SimpleDateFormat("MMM yyyy").format(cal.getTime());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("label",     monthName);
                row.put("month",     monthName);
                row.put("revenue",   0);
                row.put("bookings",  0);
                row.put("cancelled", 0);
                empty.add(row);
            }
            return ResponseEntity.ok(empty);
        }

        // Get cancellations per month too
        List<Object[]> cancelRows = em.createNativeQuery("""
            SELECT
                DATE_FORMAT(locked_at, '%b %Y') AS month,
                COUNT(*) AS cancelled
            FROM bookings
            WHERE status = 'CANCELLED'
              AND locked_at IS NOT NULL
            GROUP BY month
            """).getResultList();

        Map<String, Long> cancelMap = new HashMap<>();
        for (Object[] r : cancelRows) {
            cancelMap.put(str(r[0]), num(r[1]));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            String month = str(r[0]);  // label field
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label",     month);   // ← frontend reads "label"
            row.put("month",     month);   // ← keep "month" for compatibility
            row.put("revenue",   r[3] != null ? ((Number) r[3]).doubleValue() : 0);
            row.put("bookings",  num(r[4]));
            row.put("cancelled", cancelMap.getOrDefault(month, 0L));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }


    // ── GET /api/superadmin/analytics/bookings ────────────────
    @GetMapping("/analytics/bookings")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> bookingAnalytics() {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();

        // 1. Total counts by status
        List<Object[]> statusRows = em.createNativeQuery("""
            SELECT status, COUNT(*) AS cnt
            FROM bookings
            GROUP BY status
            """).getResultList();
        java.util.Map<String, Long> statusMap = new java.util.HashMap<>();
        for (Object[] r : statusRows) statusMap.put(str(r[0]), num(r[1]));
        result.put("totalBookings",    statusMap.values().stream().mapToLong(Long::longValue).sum());
        result.put("confirmed",        statusMap.getOrDefault("CONFIRMED", 0L));
        result.put("cancelled",        statusMap.getOrDefault("CANCELLED", 0L));
        result.put("locked",           statusMap.getOrDefault("LOCKED",    0L));
        result.put("expired",          statusMap.getOrDefault("EXPIRED",   0L));

        // 2. Booking trend — last 30 days (by locked_at date)
        List<Object[]> trendRows = em.createNativeQuery("""
            SELECT
                DATE(COALESCE(confirmed_at, locked_at)) AS day,
                COUNT(*) AS bookings,
                SUM(CASE WHEN status='CONFIRMED' THEN 1 ELSE 0 END) AS confirmed,
                SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) AS cancelled
            FROM bookings
            WHERE COALESCE(confirmed_at, locked_at) >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
            GROUP BY day
            ORDER BY day ASC
            """).getResultList();
        List<java.util.Map<String, Object>> trend = new java.util.ArrayList<>();
        for (Object[] r : trendRows) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("date",      str(r[0]));
            row.put("bookings",  num(r[1]));
            row.put("confirmed", num(r[2]));
            row.put("cancelled", num(r[3]));
            trend.add(row);
        }
        result.put("trend", trend);

        // 3. Peak booking hours (0-23)
        List<Object[]> hourRows = em.createNativeQuery("""
            SELECT
                HOUR(COALESCE(confirmed_at, locked_at)) AS hr,
                COUNT(*) AS cnt
            FROM bookings
            WHERE COALESCE(confirmed_at, locked_at) IS NOT NULL
            GROUP BY hr
            ORDER BY hr ASC
            """).getResultList();
        List<java.util.Map<String, Object>> hours = new java.util.ArrayList<>();
        for (Object[] r : hourRows) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            int h = (int) num(r[0]);
            String label = String.format("%02d:00", h);
            row.put("hour",  label);
            row.put("count", num(r[1]));
            hours.add(row);
        }
        result.put("peakHours", hours);

        // 4. Conversion rate
        long total     = (Long) result.get("totalBookings");
        long confirmed = (Long) result.get("confirmed");
        result.put("conversionRate", total > 0
            ? Math.round(((double) confirmed / total) * 100.0) : 0);

        return ResponseEntity.ok(result);
    }

    // ── GET /api/superadmin/analytics/users ───────────────────
    @GetMapping("/analytics/users")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> userAnalytics() {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();

        // 1. Total users
        long totalUsers = ((Number) em.createNativeQuery(
            "SELECT COUNT(*) FROM users").getSingleResult()).longValue();
        result.put("totalUsers", totalUsers);

        // 2. New registrations per month (last 6 months)
        // users table has id — use id range as proxy if no created_at column
        // Try created_at first, fall back to empty
        List<java.util.Map<String, Object>> regTrend = new java.util.ArrayList<>();
        // Note: users table has no created_at column — registration trend not available
        // regTrend stays empty — frontend shows "Add created_at to enable this" message
        result.put("registrationTrend", regTrend);

        // 3. Active vs Inactive users (active = has at least 1 booking)
        long activeUsers = ((Number) em.createNativeQuery("""
            SELECT COUNT(DISTINCT user_id) FROM bookings
            """).getSingleResult()).longValue();
        result.put("activeUsers",   activeUsers);
        result.put("inactiveUsers", Math.max(0, totalUsers - activeUsers));

        // 4. Top users by bookings
        List<Object[]> topRows = em.createNativeQuery("""
            SELECT
                u.id, u.name, u.email,
                COUNT(b.id)                                     AS totalBookings,
                SUM(CASE WHEN b.status='CONFIRMED' THEN 1 ELSE 0 END) AS confirmed,
                COALESCE(SUM(CASE WHEN b.status='CONFIRMED' THEN b.total_price ELSE 0 END), 0) AS spent
            FROM users u
            LEFT JOIN bookings b ON b.user_id = u.id
            GROUP BY u.id, u.name, u.email
            ORDER BY totalBookings DESC
            LIMIT 10
            """).getResultList();
        List<java.util.Map<String, Object>> topUsers = new java.util.ArrayList<>();
        for (Object[] r : topRows) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("userId",        num(r[0]));
            row.put("name",          str(r[1]));
            row.put("email",         str(r[2]));
            row.put("totalBookings", num(r[3]));
            row.put("confirmed",     num(r[4]));
            row.put("spent",         r[5] != null ? ((Number) r[5]).doubleValue() : 0.0);
            topUsers.add(row);
        }
        result.put("topUsers", topUsers);

        return ResponseEntity.ok(result);
    }

    // ── Helpers ───────────────────────────────────────────────
    private static long   num(Object o) { return o == null ? 0L : ((Number) o).longValue(); }
    private static String str(Object o) { return o == null ? "" : o.toString(); }
    // ── GET /api/superadmin/revenue/by-movie ──────────────────
    @GetMapping("/revenue/by-movie")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> revenueByMovie() {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT
                m.id          AS movieId,
                m.title       AS movieTitle,
                m.poster_url  AS posterUrl,
                COALESCE(SUM(b.total_price), 0) AS revenue,
                COUNT(b.id)                      AS bookings,
                COUNT(CASE WHEN b.status = 'CONFIRMED' THEN 1 END) AS confirmed
            FROM movies m
            LEFT JOIN shows  sh ON sh.movie_id = m.id
            LEFT JOIN bookings b ON b.show_id = sh.id AND b.status = 'CONFIRMED'
            GROUP BY m.id, m.title, m.poster_url
            ORDER BY revenue DESC
            LIMIT 20
            """).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("movieId",    num(r[0]));
            row.put("movieTitle", str(r[1]));
            row.put("posterUrl",  str(r[2]));
            row.put("revenue",    r[3] != null ? ((Number) r[3]).doubleValue() : 0.0);
            row.put("bookings",   num(r[4]));
            row.put("confirmed",  num(r[5]));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    // ── GET /api/superadmin/revenue/by-venue ──────────────────
    @GetMapping("/revenue/by-venue")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> revenueByVenue() {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT
                v.id          AS venueId,
                v.name        AS venueName,
                v.city        AS city,
                COALESCE(SUM(b.total_price), 0) AS revenue,
                COUNT(b.id)                      AS bookings,
                COUNT(DISTINCT sh.id)            AS shows
            FROM venue v
            LEFT JOIN shows sh ON sh.venue_id = v.id
            LEFT JOIN bookings b ON b.show_id = sh.id AND b.status = 'CONFIRMED'
            GROUP BY v.id, v.name, v.city
            ORDER BY revenue DESC
            LIMIT 20
            """).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("venueId",   num(r[0]));
            row.put("venueName", str(r[1]));
            row.put("city",      str(r[2]));
            row.put("revenue",   r[3] != null ? ((Number) r[3]).doubleValue() : 0.0);
            row.put("bookings",  num(r[4]));
            row.put("shows",     num(r[5]));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

}