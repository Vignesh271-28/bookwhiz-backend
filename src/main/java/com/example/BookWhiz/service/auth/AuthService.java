package com.example.BookWhiz.service.auth;

import com.example.BookWhiz.dto.request.RegisterRequest;
import com.example.BookWhiz.exception.ResourceNotFoundException;
import com.example.BookWhiz.exception.UnauthorizedException;
import com.example.BookWhiz.model.user.Role;
import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.repository.RoleRepository;
import com.example.BookWhiz.repository.UserRepository;
import com.example.BookWhiz.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    // ======================
    // SIGNUP
    // ======================
    public Map<String, String> register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UnauthorizedException("Email already registered");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() ->
                        new ResourceNotFoundException("USER role not found"));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(userRole));

        // ✅ Save first
        userRepository.save(user);

        // ✅ Generate token safely
        String token = jwtUtil.generateToken(user,user.getId());

        return Map.of(
                "message", "User registered successfully",
                "token", token
        );
    }


    // ======================
    // LOGIN
    // ======================
    // ======================
// LOGIN (FIXED VERSION)
// ======================
    public Map<String, String> login(String email, String password) {

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(email, password)
                    );

            // ✅ SAFE: get username from Authentication
            String authenticatedEmail = authentication.getName();

            // ✅ Load your actual User entity
            User user = userRepository.findByEmail(authenticatedEmail)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("User not found"));

            // ✅ Generate JWT from real user (roles included)
            String token = jwtUtil.generateToken(user, user.getId());

            return Map.of("token", token);

        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

}
