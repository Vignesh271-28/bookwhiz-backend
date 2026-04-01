package com.example.BookWhiz.controller.Notification;
import com.example.BookWhiz.model.notification.Notification;
import com.example.BookWhiz.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * GET  /api/notifications        — get all notifications for the current user
 * GET  /api/notifications/unread — unread count (for badge)
 * POST /api/notifications/read   — mark all as read
 * POST /api/notifications/{id}/read — mark one as read
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Notification> getAll(Authentication auth) {
        String email = auth.getName();
        String role  = primaryRole(auth);
        return repo.findForUser(email, role);
    }

    @GetMapping("/unread")
    public Map<String, Long> unreadCount(Authentication auth) {
        String email = auth.getName();
        String role  = primaryRole(auth);
        return Map.of("count", repo.countUnreadForUser(email, role));
    }

    @PostMapping("/read")
    public ResponseEntity<?> markAllRead(Authentication auth) {
        repo.markAllReadForUser(auth.getName(), primaryRole(auth));
        return ResponseEntity.ok(Map.of("message", "All marked as read"));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markOneRead(@PathVariable Long id) {
        repo.findById(id).ifPresent(n -> { n.setRead(true); repo.save(n); });
        return ResponseEntity.ok().build();
    }

    // ── Helper: get highest role without ROLE_ prefix ──────────
    private String primaryRole(Authentication auth) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> a.replace("ROLE_", ""))
            .findFirst()
            .orElse("USER");
    }
}
