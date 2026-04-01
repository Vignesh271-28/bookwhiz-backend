package com.example.BookWhiz.controller.superadmin;

import com.example.BookWhiz.dto.request.ShowRequest;
import com.example.BookWhiz.dto.response.ShowResponse;
import com.example.BookWhiz.service.ShowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin/shows")
@PreAuthorize("hasRole('SUPER_ADMIN','ADMIN')")
@CrossOrigin
public class SuperAdminShowController {

    private final ShowService showService;

    public SuperAdminShowController(ShowService showService) {
        this.showService = showService;
    }

    // GET all shows
    @GetMapping
    public List<ShowResponse> getAllShows() {
        return showService.getAllShows();
    }

    // GET single show
    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShow(@PathVariable Long id) {
        return showService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST create show
    @PostMapping
    public ResponseEntity<ShowResponse> createShow(@RequestBody ShowRequest request) {
            ShowResponse created = showService.createShow(request);
        return ResponseEntity.ok(created);
    }

    // PUT update show
    @PutMapping("/{id}")
    public ResponseEntity<ShowResponse> updateShow(
            @PathVariable Long id,
            @RequestBody ShowRequest request) {
        return showService.updateShow(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE show
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
        return ResponseEntity.noContent().build();
    }
}