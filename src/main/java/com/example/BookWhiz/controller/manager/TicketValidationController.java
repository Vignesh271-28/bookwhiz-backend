package com.example.BookWhiz.controller.manager;

import com.example.BookWhiz.model.booking.Booking;
import com.example.BookWhiz.model.booking.BookingStatus;
import com.example.BookWhiz.repository.BookingRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/tickets")
public class TicketValidationController {

    private final BookingRepository bookingRepository;

    public TicketValidationController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @PostMapping("/validate/{bookingId}")
    public String validate(@PathVariable Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Invalid ticket"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return "Ticket invalid or already used";
        }

        booking.setStatus(BookingStatus.CANCELLED); // mark as used
        bookingRepository.save(booking);

        return "Entry allowed ✅";
    }
}

