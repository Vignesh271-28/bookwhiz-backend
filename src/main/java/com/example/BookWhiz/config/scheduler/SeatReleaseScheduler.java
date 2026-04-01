package com.example.BookWhiz.config.scheduler;


import com.example.BookWhiz.dto.response.SeatUpdateMessage;
import com.example.BookWhiz.model.booking.Booking;
import com.example.BookWhiz.model.booking.BookingStatus;
import com.example.BookWhiz.service.websocket.SeatUpdatePublisher;
import com.example.BookWhiz.repository.BookingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SeatReleaseScheduler {

    private final BookingRepository bookingRepository;
    private final SeatUpdatePublisher seatUpdatePublisher;

    public SeatReleaseScheduler(BookingRepository bookingRepository,
                                SeatUpdatePublisher seatUpdatePublisher) {
        this.bookingRepository = bookingRepository;
        this.seatUpdatePublisher = seatUpdatePublisher;
    }

    @Scheduled(fixedRate = 60000) // runs every 1 minute
    public void releaseExpiredSeatLocks() {

        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(5);

        List<Booking> expiredBookings =
                bookingRepository.findByStatusAndLockedAtBefore(
                        BookingStatus.LOCKED, expiryTime);

        for (Booking booking : expiredBookings) {

            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            // 🔔 WebSocket broadcast (SEATS RELEASED)
            seatUpdatePublisher.broadcastSeatUpdate(
                    new SeatUpdateMessage(
                            booking.getEvent().getId(),
                            booking.getSeats()
                                    .stream()
                                    .map(seat -> seat.getId())
                                    .toList(),
                            "RELEASED"
                    )
            );
        }
    }
}


