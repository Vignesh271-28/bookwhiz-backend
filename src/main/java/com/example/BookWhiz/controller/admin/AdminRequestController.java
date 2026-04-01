package com.example.BookWhiz.controller.admin;

import com.example.BookWhiz.model.approval.PendingRequest;
import com.example.BookWhiz.model.approval.PendingRequest.RequestType;
import com.example.BookWhiz.model.approval.PendingRequestRepository;
import com.example.BookWhiz.model.notification.Notification.NotifType;
import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.repository.UserRepository;
import com.example.BookWhiz.service.EmailService;
import com.example.BookWhiz.service.NotificationService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ADMIN / MANAGER submit creation requests here.
 * They cannot create directly — only SUPER_ADMIN approves.
 *
 * POST /api/admin/requests/movie   — request to create a movie
 * POST /api/admin/requests/venue   — request to create a venue
 * POST /api/admin/requests/user    — request to create a user
 * GET  /api/admin/requests/mine    — view own submitted requests
 */
@RestController
@RequestMapping("/api/admin/requests")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPER_ADMIN')")
@CrossOrigin(origins = "*")
public class AdminRequestController {

    private final PendingRequestRepository requestRepo;
    private final UserRepository           userRepository;
    private final ObjectMapper             mapper;
    private final NotificationService      notifService;
    private final EmailService             emailService;

    public AdminRequestController(PendingRequestRepository requestRepo,
                                  UserRepository userRepository,
                                  ObjectMapper mapper,
                                  NotificationService notifService,
                                  EmailService emailService) {
        this.requestRepo    = requestRepo;
        this.userRepository = userRepository;
        this.mapper         = mapper;
        this.notifService   = notifService;
        this.emailService   = emailService;
    }

    // ── Submit movie creation request ─────────────────────────
    @PostMapping("/movie")
    public ResponseEntity<?> requestMovie(@RequestBody Map<String, Object> payload) {
        return submit(RequestType.MOVIE, payload,
            "Movie: " + payload.getOrDefault("title", "Unknown"));
    }

    // ── Submit venue creation request ─────────────────────────
    @PostMapping("/venue")
    public ResponseEntity<?> requestVenue(@RequestBody Map<String, Object> payload) {
        return submit(RequestType.VENUE, payload,
            "Venue: " + payload.getOrDefault("name", "Unknown")
            + " — " + payload.getOrDefault("city", ""));
    }

    // ── Submit user creation request ──────────────────────────
    @PostMapping("/user")
    public ResponseEntity<?> requestUser(@RequestBody Map<String, Object> payload) {
        return submit(RequestType.USER, payload,
            "User: " + payload.getOrDefault("name", "Unknown")
            + " <" + payload.getOrDefault("email", "") + ">");
    }

    // ── View own submitted requests ───────────────────────────
    @GetMapping("/mine")
    public List<PendingRequest> myRequests() {
        String email = currentEmail();
        return requestRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(r -> email.equals(r.getRequestedByEmail()))
                .toList();
    }

    // ── Internal helper ───────────────────────────────────────
    private ResponseEntity<?> submit(RequestType type,
                                     Map<String, Object> payload,
                                     String summary) {
        try {
            String email = currentEmail();
            User   user  = userRepository.findByEmail(email).orElse(null);
            String name  = user != null ? user.getName() : email;

            PendingRequest req = new PendingRequest();
            req.setType(type);
            req.setPayload(mapper.writeValueAsString(payload));
            req.setSummary(summary);
            req.setRequestedByEmail(email);
            req.setRequestedByName(name);

            requestRepo.save(req);

            // ── Notify all SUPER_ADMINs (WebSocket + Email) ───────
            notifService.notifyRole(
                NotifType.REQUEST_SUBMITTED,
                "SUPER_ADMIN",
                "📥 New " + type.name() + " Request",
                name + " submitted a request: " + summary
            );

            // Email every SUPER_ADMIN
            userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                    .anyMatch(r -> r.getName().contains("SUPER_ADMIN")))
                .forEach(admin -> emailService.sendNewRequestAlert(
                    admin.getEmail(), name, type.name(), summary));

            return ResponseEntity.ok(Map.of(
                "message", "Request submitted. Awaiting SuperAdmin approval.",
                "requestId", req.getId(),
                "status",    "PENDING"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to submit request: " + e.getMessage()));
        }
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}