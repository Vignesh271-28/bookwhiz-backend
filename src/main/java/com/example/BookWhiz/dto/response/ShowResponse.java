package com.example.BookWhiz.dto.response;

import com.example.BookWhiz.model.show.Show;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ShowResponse {

    private Long id;

    // @JsonFormat here as belt-and-suspenders: even if the Show entity
    // somehow loses its own @JsonFormat, this DTO's serialization stays correct.
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate showDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime showTime;

    private Double  price;
    private Integer totalSeats;

    private MovieInfo movie;
    private VenueInfo venue;

    @Data
    public static class MovieInfo {
        private Long   id;
        private String title;
        private String genre;
        private String language;
        private String format;
    }

    @Data
    public static class VenueInfo {
        private Long   id;
        private String name;
        private String city;
        private String area;
    }

    // ── Build from JPA entity ─────────────────────────────────
    public static ShowResponse from(Show show) {
        ShowResponse res = new ShowResponse();
        res.setId(show.getId());
        res.setShowDate(show.getShowDate());
        res.setShowTime(show.getShowTime());
        res.setPrice(show.getPrice());
        res.setTotalSeats(show.getTotalSeats());

        if (show.getMovie() != null) {
            MovieInfo mi = new MovieInfo();
            mi.setId(show.getMovie().getId());
            mi.setTitle(show.getMovie().getTitle());
            mi.setGenre(show.getMovie().getGenre());
            mi.setLanguage(show.getMovie().getLanguage());
            mi.setFormat(show.getMovie().getFormat());
            res.setMovie(mi);
        }

        if (show.getVenue() != null) {
            VenueInfo vi = new VenueInfo();
            vi.setId(show.getVenue().getId());
            vi.setName(show.getVenue().getName());
            vi.setCity(show.getVenue().getCity());
            vi.setArea(show.getVenue().getArea());
            res.setVenue(vi);
        }

        return res;
    }
}