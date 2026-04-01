package com.example.BookWhiz.controller.superadmin;

import com.example.BookWhiz.model.approval.PendingRequest;
import com.example.BookWhiz.model.approval.PendingRequest.RequestStatus;
import com.example.BookWhiz.model.approval.PendingRequestRepository;
import com.example.BookWhiz.model.movie.Movie;
import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.model.venue.Venue;
import com.example.BookWhiz.model.notification.Notification.NotifType;
import com.example.BookWhiz.service.movie.MovieService;
import com.example.BookWhiz.service.VenueService;
import com.example.BookWhiz.service.user.UserService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.BookWhiz.service.EmailService;
import com.example.BookWhiz.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * SUPER_ADMIN approves or rejects pending requests from ADMIN/MANAGER.
 * ADMIN can VIEW all requests (read-only).
 *
 * GET    /api/superadmin/requests              — all requests (SUPER_ADMIN + ADMIN)
 * GET    /api/superadmin/requests/pending      — only pending  (SUPER_ADMIN + ADMIN)
 * GET    /api/superadmin/requests/count        — pending count (SUPER_ADMIN + ADMIN)
 * POST   /api/superadmin/requests/{id}/approve — SUPER_ADMIN only
 * POST   /api/superadmin/requests/{id}/reject  — SUPER_ADMIN only
 */
@RestController
@RequestMapping("/api/superadmin/requests")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")   // ✅ ADMIN can now read
@CrossOrigin(origins = "*")
public class SuperAdminApprovalController {

    private final PendingRequestRepository requestRepo;
    private final MovieService             movieService;
    private final VenueService             venueService;
    private final UserService              userService;
    private final ObjectMapper             mapper;
    private final NotificationService      notifService;
    private final EmailService             emailService;

    public SuperAdminApprovalController(PendingRequestRepository requestRepo,
                                        MovieService movieService,
                                        VenueService venueService,
                                        UserService userService,
                                        ObjectMapper mapper,
                                        NotificationService notifService,
                                        EmailService emailService) {
        this.requestRepo  = requestRepo;
        this.movieService = movieService;
        this.venueService = venueService;
        this.userService  = userService;
        this.mapper       = mapper;
        this.notifService = notifService;
        this.emailService = emailService;
    }

    // ── GET /api/superadmin/requests ─────────────────────────
    // Both SUPER_ADMIN and ADMIN can view all requests
    @GetMapping
    public List<PendingRequest> getAllRequests() {
        return requestRepo.findAllByOrderByCreatedAtDesc();
    }

    // ── GET /api/superadmin/requests/pending ─────────────────
    // Both SUPER_ADMIN and ADMIN can view pending requests
    @GetMapping("/pending")
    public List<PendingRequest> getPending() {
        return requestRepo.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING);
    }

    // ── GET /api/superadmin/requests/count ───────────────────
    // Both SUPER_ADMIN and ADMIN can see the pending badge count
    @GetMapping("/count")
    public Map<String, Long> getPendingCount() {
        return Map.of("pending", requestRepo.countByStatus(RequestStatus.PENDING));
    }

    // ── POST /api/superadmin/requests/{id}/approve ───────────
    // SUPER_ADMIN only — overrides the class-level annotation
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        PendingRequest req = requestRepo.findById(id).orElse(null);
        if (req == null)
            return ResponseEntity.notFound().build();
        if (req.getStatus() != RequestStatus.PENDING)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Request is already " + req.getStatus()));

        try {
            Object created = switch (req.getType()) {
                case MOVIE -> {
                    Movie movie = mapper.readValue(req.getPayload(), Movie.class);
                    yield movieService.createMovie(movie);
                }
                case VENUE -> {
                    Venue venue = mapper.readValue(req.getPayload(), Venue.class);
                    yield venueService.createVenue(venue);
                }
                case USER -> {
                    User user = mapper.readValue(req.getPayload(), User.class);
                    yield userService.createUser(user);
                }
            };

            req.setStatus(RequestStatus.APPROVED);
            req.setReviewedAt(LocalDateTime.now());
            if (body != null) req.setReviewNote(body.get("note"));
            requestRepo.save(req);

            // ── Notify requester ──────────────────────────────
            emailService.sendRequestApproved(
                req.getRequestedByEmail(),
                req.getRequestedByName() != null
                    ? req.getRequestedByName()
                    : req.getRequestedByEmail(),
                req.getType().name(),
                req.getSummary()
            );

            notifService.notifyUser(
                NotifType.REQUEST_APPROVED,
                req.getRequestedByEmail(),
                "✅ Request Approved",
                "Your request \"" + req.getSummary() + "\" has been approved and created."
            );

            return ResponseEntity.ok(Map.of(
                "message", "Approved and created successfully.",
                "created", created
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Approval failed: " + e.getMessage()));
        }
    }

    // ── POST /api/superadmin/requests/{id}/reject ────────────
    // SUPER_ADMIN only — overrides the class-level annotation
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                    @RequestBody(required = false) Map<String, String> body) {
        PendingRequest req = requestRepo.findById(id).orElse(null);
        if (req == null)
            return ResponseEntity.notFound().build();
        if (req.getStatus() != RequestStatus.PENDING)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Request is already " + req.getStatus()));

        req.setStatus(RequestStatus.REJECTED);
        req.setReviewedAt(LocalDateTime.now());
        req.setReviewNote(body != null ? body.get("note") : null);
        requestRepo.save(req);

        // ── Notify requester ──────────────────────────────────
        notifService.notifyUser(
            NotifType.REQUEST_REJECTED,
            req.getRequestedByEmail(),
            "❌ Request Rejected",
            "Your request \"" + req.getSummary() + "\" was rejected." +
            (req.getReviewNote() != null ? " Reason: " + req.getReviewNote() : "")
        );

        return ResponseEntity.ok(Map.of("message", "Request rejected."));
    }
}