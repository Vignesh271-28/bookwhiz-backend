package com.example.BookWhiz.service.impl;

import com.example.BookWhiz.dto.request.ShowRequest;
import com.example.BookWhiz.dto.response.ShowResponse;
import com.example.BookWhiz.model.movie.Movie;
import com.example.BookWhiz.model.show.Show;
import com.example.BookWhiz.model.venue.Venue;
import com.example.BookWhiz.repository.BookingRepository;
import com.example.BookWhiz.repository.MovieRepository;
import com.example.BookWhiz.repository.ShowRepository;
import com.example.BookWhiz.repository.VenueRepository;
import com.example.BookWhiz.service.ShowService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ShowServiceImpl implements ShowService {

    private final ShowRepository    showRepository;
    private final MovieRepository   movieRepository;
    private final VenueRepository   venueRepository;
    private final EntityManager     em;
    private final BookingRepository bookingRepository;

    public ShowServiceImpl(ShowRepository showRepository,
                           MovieRepository movieRepository,
                           VenueRepository venueRepository,
                           EntityManager em,
                           BookingRepository bookingRepository) {
        this.showRepository    = showRepository;
        this.movieRepository   = movieRepository;
        this.venueRepository   = venueRepository;
        this.em                = em;
        this.bookingRepository = bookingRepository;
    }

    // ── getAllShows via native SQL ─────────────────────────────
    // JPQL LEFT JOIN FETCH still crashes on orphan FK (movie_id=25 deleted).
    // Native SQL: LEFT JOIN returns NULL columns — no proxy, no exception.
    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<ShowResponse> getAllShows() {

        List<Object[]> rows = em.createNativeQuery("""
            SELECT
                s.id          AS id,
                s.show_date   AS showDate,
                s.show_time   AS showTime,
                s.price       AS price,
                s.total_seats AS totalSeats,
                m.id          AS movieId,
                m.title       AS movieTitle,
                m.genre       AS movieGenre,
                m.language    AS movieLanguage,
                m.format      AS movieFormat,
                v.id          AS venueId,
                v.name        AS venueName,
                v.city        AS venueCity,
                v.area        AS venueArea
            FROM shows s
            LEFT JOIN movies m ON m.id = s.movie_id
            LEFT JOIN venue  v ON v.id = s.venue_id
            ORDER BY s.id DESC
            """).getResultList();

        List<ShowResponse> result = new ArrayList<>();
        for (Object[] r : rows) {
            ShowResponse res = new ShowResponse();
            res.setId(toLong(r[0]));
            res.setShowDate(toLocalDate(r[1]));
            res.setShowTime(toLocalTime(r[2]));
            res.setPrice(r[3] != null ? ((Number) r[3]).doubleValue() : null);
            res.setTotalSeats(r[4] != null ? ((Number) r[4]).intValue() : null);

            if (r[5] != null) {
                ShowResponse.MovieInfo mi = new ShowResponse.MovieInfo();
                mi.setId(toLong(r[5]));
                mi.setTitle(str(r[6]));
                mi.setGenre(str(r[7]));
                mi.setLanguage(str(r[8]));
                mi.setFormat(str(r[9]));
                res.setMovie(mi);
            }

            if (r[10] != null) {
                ShowResponse.VenueInfo vi = new ShowResponse.VenueInfo();
                vi.setId(toLong(r[10]));
                vi.setName(str(r[11]));
                vi.setCity(str(r[12]));
                vi.setArea(str(r[13]));
                res.setVenue(vi);
            }

            result.add(res);
        }
        return result;
    }

    @Override
    public Optional<ShowResponse> findById(Long id) {
        return showRepository.findById(id).map(ShowResponse::from);
    }

    @Override
    public ShowResponse createShow(ShowRequest request) {
        Show show = buildShow(new Show(), request);
        return ShowResponse.from(showRepository.save(show));
    }

    @Override
    public Optional<ShowResponse> updateShow(Long id, ShowRequest request) {
        return showRepository.findById(id).map(existing -> {
            buildShow(existing, request);
            return ShowResponse.from(showRepository.save(existing));
        });
    }

    @Override
    @Transactional
    public void deleteShow(Long id) {
        // 1. Delete payments (FK → bookings)
        em.createNativeQuery("""
            DELETE FROM payments
            WHERE booking_id IN (
                SELECT id FROM bookings WHERE show_id = ?1
            )
            """).setParameter(1, id).executeUpdate();

        // 2. Delete booking_seats (FK → bookings)
        em.createNativeQuery("""
            DELETE FROM booking_seats
            WHERE booking_id IN (
                SELECT id FROM bookings WHERE show_id = ?1
            )
            """).setParameter(1, id).executeUpdate();

        // 3. Delete bookings (FK → shows)
        em.createNativeQuery("DELETE FROM bookings WHERE show_id = ?1")
            .setParameter(1, id).executeUpdate();

        // 4. Now safe to delete the show
        showRepository.deleteById(id);
    }

    // ── Build Show entity from ShowRequest ────────────────────
    private Show buildShow(Show show, ShowRequest req) {

        if (req.getMovieId() != null) {
            Movie movie = movieRepository.findById(req.getMovieId())
                    .orElseThrow(() -> new RuntimeException("Movie not found: " + req.getMovieId()));
            show.setMovie(movie);
        }

        if (req.getVenueId() != null) {
            Venue venue = venueRepository.findById(req.getVenueId())
                    .orElseThrow(() -> new RuntimeException("Venue not found: " + req.getVenueId()));
            show.setVenue(venue);
        }

        if (req.getShowDate() != null && !req.getShowDate().isBlank())
            show.setShowDate(LocalDate.parse(req.getShowDate()));

        if (req.getShowTime() != null && !req.getShowTime().isBlank())
            show.setShowTime(LocalTime.parse(req.getShowTime()));

        if (req.getPrice()      != null) show.setPrice(req.getPrice());
        if (req.getTotalSeats() != null) show.setTotalSeats(req.getTotalSeats());

        return show;
    }

    // ── Type helpers ──────────────────────────────────────────
    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(o.toString());
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }

    private static LocalDate toLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate ld) return ld;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        return LocalDate.parse(o.toString());
    }

    private static LocalTime toLocalTime(Object o) {
        if (o == null) return null;
        if (o instanceof LocalTime lt) return lt;
        if (o instanceof java.sql.Time t) return t.toLocalTime();
        String s = o.toString();
        if (s.contains(":")) {
            String[] parts = s.split(":");
            return LocalTime.of(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                parts.length > 2 ? Integer.parseInt(parts[2].split("\\.")[0]) : 0
            );
        }
        return LocalTime.parse(s);
    }
}