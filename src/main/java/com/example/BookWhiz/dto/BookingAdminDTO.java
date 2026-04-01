// package com.example.BookWhiz.dto;

// import com.example.BookWhiz.model.booking.Booking;
// import com.example.BookWhiz.model.booking.BookingStatus;

// import java.time.LocalDateTime;
// import java.util.Collections;
// import java.util.List;

// /**
//  * Flat DTO — breaks Jackson circular-reference:
//  *   Booking → User  → List<Booking> (infinite)
//  *   Booking → Show  → List<Booking> (infinite)
//  *
//  * Drop in: src/main/java/com/example/BookWhiz/dto/BookingAdminDTO.java
//  * (create the dto/ folder if it doesn't exist)
//  */
// public class BookingAdminDTO {

//     // ── Booking ───────────────────────────────────────────────
//     public Long          id;
//     public BookingStatus status;
//     public Double        totalPrice;
//     public LocalDateTime bookingTime;
//     public LocalDateTime lockedAt;

//     // ── User (flat) ───────────────────────────────────────────
//     public Long   userId;
//     public String userName;
//     public String userEmail;

//     // ── Show (flat) ───────────────────────────────────────────
//     public Long   showId;
//     public String showDate;   // stored as String to avoid date-format issues
//     public String showTime;
//     public Double showPrice;

//     // ── Movie (flat) ──────────────────────────────────────────
//     public Long   movieId;
//     public String movieTitle;
//     public String moviePosterUrl;

//     // ── Venue (flat) ──────────────────────────────────────────
//     public Long   venueId;
//     public String venueName;
//     public String venueCity;
//     public String venueArea;

//     // ── Seats (flat list) ─────────────────────────────────────
//     public List<SeatInfo> seats;

//     public static class SeatInfo {
//         public Long   id;
//         public String seatNumber;
//         public String seatType;

//         public SeatInfo(Long id, String seatNumber, String seatType) {
//             this.id         = id;
//             this.seatNumber = seatNumber;
//             this.seatType   = seatType;
//         }
//     }

//     // ── Static factory ────────────────────────────────────────
//     public static BookingAdminDTO from(Booking b) {
//         BookingAdminDTO d = new BookingAdminDTO();
//         d.id          = b.getId();
//         d.status      = b.getStatus();
//         d.totalPrice  = b.getTotalPrice();
//         d.lockedAt    = b.getLockedAt();

//         // bookingTime — field may be named differently in your entity.
//         // If you get a compile error here, check your Booking entity for the
//         // actual field name (e.g. bookedAt / createdAt) and update this line.
//         try { d.bookingTime = b.getBookingTime(); } catch (Exception ignored) {}

//         if (b.getUser() != null) {
//             d.userId    = b.getUser().getId();
//             d.userName  = b.getUser().getName();
//             d.userEmail = b.getUser().getEmail();
//         }

//         if (b.getShow() != null) {
//             d.showId    = b.getShow().getId();
//             d.showDate  = b.getShow().getShowDate()  != null
//                           ? b.getShow().getShowDate().toString()  : null;
//             d.showTime  = b.getShow().getShowTime()  != null
//                           ? b.getShow().getShowTime().toString()  : null;
//             d.showPrice = b.getShow().getPrice();

//             if (b.getShow().getMovie() != null) {
//                 d.movieId        = b.getShow().getMovie().getId();
//                 d.movieTitle     = b.getShow().getMovie().getTitle();
//                 d.moviePosterUrl = b.getShow().getMovie().getPosterUrl();
//             }

//             if (b.getShow().getVenue() != null) {
//                 d.venueId   = b.getShow().getVenue().getId();
//                 d.venueName = b.getShow().getVenue().getName();
//                 d.venueCity = b.getShow().getVenue().getCity();
//                 d.venueArea = b.getShow().getVenue().getArea();
//             }
//         }

//         if (b.getSeats() != null) {
//             d.seats = b.getSeats().stream()
//                 .map(s -> new SeatInfo(
//                     s.getId(),
//                     s.getSeatNumber(),
//                     s.getSeatType() != null ? s.getSeatType().toString() : null
//                 ))
//                 .toList();
//         } else {
//             d.seats = Collections.emptyList();
//         }

//         return d;
//     }
// }