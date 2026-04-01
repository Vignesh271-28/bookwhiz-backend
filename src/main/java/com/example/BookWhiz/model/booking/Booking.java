package com.example.BookWhiz.model.booking;

import com.example.BookWhiz.model.event.Event;
import com.example.BookWhiz.model.show.Show;
import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.model.venue.Seat;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Event event;

    @ManyToOne
    @JoinColumn(name = "show_id")
    private Show show;

    @ManyToMany
    @JoinTable(
            name = "booking_seats",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "seat_id")
    )
    private List<Seat> seats;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDateTime lockedAt;

    // ── Added fields ──────────────────────────────────────────
    // total_price: base show price × number of seats booked.
    // Set in BookingController.lockSeats() so revenue queries work.
    @Column(name = "total_price")
    private Double totalPrice;

    // confirmed_at: timestamp when booking moves to CONFIRMED.
    // Used by SuperAdminStatsController for monthly revenue grouping.
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    public Booking() {}

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId()                      { return id; }
    public void setId(Long id)               { this.id = id; }

    public User getUser()                    { return user; }
    public void setUser(User user)           { this.user = user; }

    public Event getEvent()                  { return event; }
    public void setEvent(Event event)        { this.event = event; }

    public Show getShow()                    { return show; }
    public void setShow(Show show)           { this.show = show; }

    public List<Seat> getSeats()             { return seats; }
    public void setSeats(List<Seat> seats)   { this.seats = seats; }

    public BookingStatus getStatus()                   { return status; }
    public void setStatus(BookingStatus status)        { this.status = status; }

    public LocalDateTime getLockedAt()                 { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt)    { this.lockedAt = lockedAt; }

    public Double getTotalPrice()                      { return totalPrice; }
    public void setTotalPrice(Double totalPrice)       { this.totalPrice = totalPrice; }

    public LocalDateTime getConfirmedAt()              { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
}