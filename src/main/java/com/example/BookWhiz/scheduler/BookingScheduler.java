package com.example.BookWhiz.scheduler;

import com.example.BookWhiz.model.booking.Booking;
import com.example.BookWhiz.model.booking.BookingStatus;
import com.example.BookWhiz.model.show.Show;
import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.repository.BookingRepository;
import com.example.BookWhiz.service.EmailService;
import com.example.BookWhiz.service.seat.SeatLockService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduled job — runs every 2 minutes.
 *
 * Finds LOCKED bookings older than 30 minutes (unpaid),
 * cancels them, unlocks the seats in Redis, and emails the user.
 */
@Component
public class BookingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingScheduler.class);
    private static final int LOCK_TIMEOUT_MINUTES = 30;

    private final BookingRepository bookingRepository;
    private final SeatLockService   seatLockService;
    private final EmailService      emailService;

    public BookingScheduler(BookingRepository bookingRepository,
                            SeatLockService   seatLockService,
                            EmailService      emailService) {
        this.bookingRepository = bookingRepository;
        this.seatLockService   = seatLockService;
        this.emailService      = emailService;
    }

    /**
     * Runs every 2 minutes.
     * Cancels any LOCKED booking that was not confirmed within 30 minutes.
     */
    @Scheduled(fixedDelay = 2 * 60 * 1000)   // every 2 minutes
    @Transactional
    public void cancelExpiredLocks() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(LOCK_TIMEOUT_MINUTES);

        List<Booking> expired = bookingRepository
                .findByStatusAndLockedAtBefore(BookingStatus.LOCKED, cutoff);

        if (expired.isEmpty()) return;

        log.info("BookingScheduler: cancelling {} expired locked bookings", expired.size());

        for (Booking booking : expired) {
            try {
                // 1. Unlock seats in Redis so others can book
                Show      show    = booking.getShow();
                List<Long> seatIds = booking.getSeats()
                        .stream()
                        .map(s -> s.getId())
                        .toList();
                if (show != null && !seatIds.isEmpty()) {
                    seatLockService.unlockSeats(show.getId(), seatIds);
                }

                // 2. Mark booking as EXPIRED
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);

                // 3. Email the user
                User user = booking.getUser();
                if (user != null && user.getEmail() != null) {
                    String movieTitle = (show != null && show.getMovie() != null)
                            ? show.getMovie().getTitle() : "Your show";
                    String showDate = (show != null && show.getShowDate() != null)
                            ? show.getShowDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
                    String venueName = (show != null && show.getVenue() != null)
                            ? show.getVenue().getName() : "—";

                    emailService.sendBookingExpired(
                            user.getEmail(),
                            user.getName() != null ? user.getName() : user.getEmail(),
                            movieTitle, showDate, venueName
                    );
                }

                log.debug("Expired booking #{} for user {}", booking.getId(),
                        user != null ? user.getEmail() : "unknown");

            } catch (Exception e) {
                log.error("Error expiring booking #{}: {}", booking.getId(), e.getMessage());
            }
        }

        log.info("BookingScheduler: done. {} bookings expired.", expired.size());
    }
}