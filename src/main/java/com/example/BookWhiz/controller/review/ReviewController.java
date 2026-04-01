package com.example.BookWhiz.controller.review;

import com.example.BookWhiz.service.review.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.example.BookWhiz.dto.request.ReviewRequest;
import com.example.BookWhiz.dto.response.ReviewResponse;
import com.example.BookWhiz.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin
public class ReviewController {

    private final ReviewService    reviewService;
    private final UserRepository   userRepository;

    public ReviewController(ReviewService  reviewService,
                            UserRepository userRepository) {
        this.reviewService  = reviewService;
        this.userRepository = userRepository;
    }

    // ── GET reviews for a movie (public — all roles) ──────────
    @GetMapping("/movie/{movieId}")
    public List<ReviewResponse> getReviews(@PathVariable Long movieId) {
        return reviewService.getReviewsByMovie(movieId);
    }

    // ── POST add review (any logged-in user) ──────────────────
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> addReview(@RequestBody ReviewRequest request) {
        Long userId = resolveUserId();
        ReviewResponse saved = reviewService.addReview(userId, request);
        return ResponseEntity.ok(saved);
    }

    // ── DELETE review (owner OR admin / super_admin) ──────────
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        Long userId = resolveUserId();
        String role   = resolveRole();
        reviewService.deleteReview(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────
    // Helpers — get current user id & role from SecurityContext
    // ─────────────────────────────────────────────────────────
    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = ((UserDetails) auth.getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    private String resolveRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_USER");
    }
}