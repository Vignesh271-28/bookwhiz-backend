package com.example.BookWhiz.controller;

import com.example.BookWhiz.model.partner.PartnerApplication;
import com.example.BookWhiz.model.partner.PartnerApplication.AppStatus;
import com.example.BookWhiz.repository.PartnerApplicationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * PUBLIC — no authentication required.
 *
 * POST /api/partner/apply           – theatre owner submits partnership application
 * GET  /api/partner/status/{email}  – check application status by email
 */
@RestController
@RequestMapping("/api/partner")
@CrossOrigin(origins = "*")
public class PartnerController {

    private final PartnerApplicationRepository repo;

    public PartnerController(PartnerApplicationRepository repo) {
        this.repo = repo;
    }

    // ── Submit application ────────────────────────────────────
    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody PartnerApplication req) {
        if (repo.existsByEmail(req.getEmail()))
            return ResponseEntity.badRequest()
                .body(Map.of("error", "An application with this email already exists."));

        if (blank(req.getName()))        return bad("Full name is required");
        if (blank(req.getEmail()))       return bad("Email is required");
        if (!req.getEmail().matches(".*@.*\\..*")) return bad("Invalid email address");
        if (blank(req.getPhone()))       return bad("Phone number is required");
        if (blank(req.getTheatreName())) return bad("Theatre / company name is required");
        if (blank(req.getCity()))        return bad("City is required");
        if (blank(req.getAddress()))     return bad("Address is required");

        req.setStatus(AppStatus.PENDING);
        PartnerApplication saved = repo.save(req);

        return ResponseEntity.ok(Map.of(
            "message",       "Application submitted! We'll review it within 2–3 business days.",
            "applicationId", saved.getId(),
            "status",        "PENDING"
        ));
    }

    // ── Check status ──────────────────────────────────────────
    @GetMapping("/status/{email}")
    public ResponseEntity<?> checkStatus(@PathVariable String email) {
        return repo.findByEmail(email)
            .<ResponseEntity<?>>map(a -> ResponseEntity.ok(Map.of(
                "status",      a.getStatus(),
                "theatreName", a.getTheatreName() == null ? "" : a.getTheatreName(),
                "submittedAt", a.getCreatedAt().toString(),
                "reviewNote",  a.getReviewNote() == null ? "" : a.getReviewNote()
            )))
            .orElse(ResponseEntity.status(404)
                .body(Map.of("error", "No application found for this email")));
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
    private ResponseEntity<?> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }
}