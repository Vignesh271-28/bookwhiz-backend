    package com.example.BookWhiz.service.booking;

    import com.example.BookWhiz.dto.SeatUpdate;
    import com.example.BookWhiz.exception.ResourceNotFoundException;
    import com.example.BookWhiz.model.booking.Booking;
    import com.example.BookWhiz.model.booking.BookingStatus;
    import com.example.BookWhiz.model.venue.Seat;
    import com.example.BookWhiz.repository.BookingRepository;
    import com.example.BookWhiz.service.seat.SeatLockService;
    import org.springframework.messaging.simp.SimpMessagingTemplate;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.List;

    @Service
    public class BookingServiceImpl implements BookingServiceInter {

        private final BookingRepository bookingRepository;
        private final SeatLockService seatLockService;
        private final SimpMessagingTemplate messagingTemplate;

        public BookingServiceImpl(
                BookingRepository bookingRepository,
                SeatLockService seatLockService,
                SimpMessagingTemplate messagingTemplate
        ) {
            this.bookingRepository = bookingRepository;
            this.seatLockService = seatLockService;
            this.messagingTemplate = messagingTemplate;
        }

        /* 🔹 Get booking by ID */
        @Override
        public Booking getBookingById(Long bookingId) {
            return bookingRepository.findById(bookingId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Booking not found"));
        }

        /* 🔹 Get ALL bookings (Manager / Admin / SuperAdmin) */
        @Override
        public List<Booking> getAllBookings() {
            return bookingRepository.findAll();
        }

        /* 🔹 Confirm booking (LOCKED → CONFIRMED) */
        @Override
        @Transactional
        public Booking confirmBooking(Long bookingId) {

            Booking booking = getBookingById(bookingId);

            if (booking.getStatus() != BookingStatus.LOCKED) {
                throw new IllegalStateException(
                        "Only LOCKED bookings can be confirmed"
                );
            }

            Long showId = booking.getShow().getId();
            Long userId = booking.getUser().getId();

            List<Long> seatIds = booking.getSeats()
                    .stream()
                    .map(Seat::getId)
                    .toList();

            // ✅ FINALIZE booking
            booking.setStatus(BookingStatus.CONFIRMED);
            Booking saved = bookingRepository.save(booking);

            // 🔓 Remove Redis locks
            seatLockService.unlockSeats(showId, seatIds);

            // 📢 Broadcast BOOKED
            seatIds.forEach(seatId ->
                    messagingTemplate.convertAndSend(
                            "/topic/seats/" + showId,
                            new SeatUpdate(seatId, "BOOKED", userId)
                    )
            );

            return saved;
        }

        /* 🔹 Cancel booking */
        @Override
        @Transactional
        public Booking cancelBooking(Long bookingId) {

            Booking booking = getBookingById(bookingId);

            if (booking.getStatus() != BookingStatus.LOCKED) {
                throw new IllegalStateException("Only LOCKED bookings can be cancelled");
            }

            Long showId = booking.getShow().getId();
            List<Long> seatIds = booking.getSeats()
                    .stream().map(Seat::getId).toList();

            booking.setStatus(BookingStatus.CANCELLED);
            Booking saved = bookingRepository.save(booking);

            seatLockService.unlockSeats(showId, seatIds);

            return saved;
        }

        /* 🔹 Expire booking (system / scheduler) */
        @Override
        @Transactional
        public Booking expireBooking(Long bookingId) {

            Booking booking = getBookingById(bookingId);

            if (booking.getStatus() != BookingStatus.LOCKED) {
                return booking;
            }

            Long showId = booking.getShow().getId();
            List<Long> seatIds = booking.getSeats()
                    .stream().map(Seat::getId).toList();

            booking.setStatus(BookingStatus.EXPIRED);
            Booking saved = bookingRepository.save(booking);

            seatLockService.unlockSeats(showId, seatIds);

            return saved;
        }
    }