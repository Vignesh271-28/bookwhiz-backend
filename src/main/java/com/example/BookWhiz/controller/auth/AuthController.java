package com.example.BookWhiz.controller.auth;

import com.example.BookWhiz.dto.request.RegisterRequest;
import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.repository.UserRepository;
import com.example.BookWhiz.service.auth.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    // ✅ SIGNUP
    @PostMapping("/register")
    public Map<String, String> register(
            @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    public User me(Authentication authentication) {
        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow();
    }

@PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> request) {
        return authService.login(request.get("email"), request.get("password"));
    }
}

