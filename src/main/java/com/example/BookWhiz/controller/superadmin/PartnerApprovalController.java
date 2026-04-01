package com.example.BookWhiz.controller.superadmin;

import com.example.BookWhiz.model.partner.PartnerApplication;
import com.example.BookWhiz.model.partner.PartnerApplication.AppStatus;
import com.example.BookWhiz.model.user.Role;
import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.repository.PartnerApplicationRepository;
import com.example.BookWhiz.repository.RoleRepository;
import com.example.BookWhiz.service.EmailService;
import com.example.BookWhiz.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/superadmin/partners")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@CrossOrigin(origins = "*")
public class PartnerApprovalController {

    private final PartnerApplicationRepository appRepo;
    private final UserService                  userService;
    private final RoleRepository               roleRepository;
    private final PasswordEncoder              passwordEncoder;
    private final EmailService                 emailService;

    public PartnerApprovalController(PartnerApplicationRepository appRepo,
                                     UserService userService,
                                     RoleRepository roleRepository,
                                     PasswordEncoder passwordEncoder,
                                     EmailService emailService) {
        this.appRepo         = appRepo;
        this.userService     = userService;
        this.roleRepository  = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService    = emailService;
    }

    @GetMapping
    public List<PartnerApplication> getAll() {
        return appRepo.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/pending")
    public List<PartnerApplication> getPending() {
        return appRepo.findByStatusOrderByCreatedAtDesc(AppStatus.PENDING);
    }

    @GetMapping("/count")
    public Map<String, Long> getCount() {
        return Map.of("pending", appRepo.countByStatus(AppStatus.PENDING));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        PartnerApplication app = appRepo.findById(id).orElse(null);
        if (app == null) return ResponseEntity.notFound().build();
        if (app.getStatus() != AppStatus.PENDING)
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Application is already " + app.getStatus()));

        // Check if user already exists with this email
        if (userService.existsByEmail(app.getEmail())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "A user with email " + app.getEmail() + " already exists."));
        }

        try {
            // Fetch the MANAGER role from DB — never create a new Role entity
            Role managerRole = roleRepository.findByName("MANAGER")
                .or(() -> roleRepository.findByName("ROLE_MANAGER"))
                .orElseThrow(() -> new RuntimeException(
                    "MANAGER role not found in DB. Check your roles table."));

            User user = new User();
            user.setName(app.getName());
            user.setEmail(app.getEmail());
            // Encode password — phone number as temp password
            // Use the password the partner set during registration
            // Fall back to phone number if password field is empty (old applications)
            String rawPassword = (app.getPassword() != null && !app.getPassword().isBlank())
                    ? app.getPassword()
                    : app.getPhone();
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRoles(new java.util.HashSet<>(java.util.List.of(managerRole)));

            User created = userService.saveUser(user);   // save directly, password already encoded

            app.setStatus(AppStatus.APPROVED);
            app.setReviewedAt(LocalDateTime.now());
            app.setCreatedUserId(created.getId());
            if (body != null && body.get("note") != null) app.setReviewNote(body.get("note"));
            appRepo.save(app);

            // Email the partner with approval notification
            emailService.sendPartnerApproved(app.getEmail(), app.getName(), app.getTheatreName());

            return ResponseEntity.ok(Map.of(
                "message", "Partner approved. Account created for " + app.getEmail(),
                "userId",  created.getId(),
                "info",    "Login with the password you set during registration."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to create account: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                    @RequestBody(required = false) Map<String, String> body) {
        PartnerApplication app = appRepo.findById(id).orElse(null);
        if (app == null) return ResponseEntity.notFound().build();
        if (app.getStatus() != AppStatus.PENDING)
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Application is already " + app.getStatus()));

        String note = (body != null) ? body.get("note") : null;
        if (note == null || note.isBlank())
            return ResponseEntity.badRequest()
                .body(Map.of("error", "A rejection reason is required."));

        app.setStatus(AppStatus.REJECTED);
        app.setReviewNote(note);
        app.setReviewedAt(LocalDateTime.now());
        appRepo.save(app);

        // Email the partner with rejection reason
        emailService.sendPartnerRejected(app.getEmail(), app.getName(), app.getTheatreName(), note);

        return ResponseEntity.ok(Map.of("message", "Application rejected."));
    }
}