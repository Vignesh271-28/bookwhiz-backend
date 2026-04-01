package com.example.BookWhiz.controller;


import com.example.BookWhiz.dto.request.SeatLockRequest;
import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.repository.UserRepository;
import com.example.BookWhiz.service.seat.SeatLockService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
public class SeatLockController {

    private final SeatLockService seatLockService;
    private final UserRepository userRepository;

    public SeatLockController(
            SeatLockService seatLockService,
            UserRepository userRepository
    ) {
        this.seatLockService = seatLockService;
        this.userRepository = userRepository;
    }
    @PostMapping("/lock")
    public ResponseEntity<?> lockSeats(
            @RequestParam Long showId,
            @RequestBody List<Long> seatIds,
            Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        boolean locked = seatLockService.lockSeatsAtomically(
                showId, seatIds, user.getId()
        );

        if (!locked) {
            return ResponseEntity.status(409)
                    .body("One or more seats already locked");
        }

        return ResponseEntity.ok("Seats locked");
    }
}