package com.example.BookWhiz.service.user;

import com.example.BookWhiz.model.user.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    // ── Already in your service (keep these) ─────────────────
    User createUser(User user);

    // updateUser — change return type to Optional<User> for proper 404 handling
    Optional<User> updateUser(Long id, User user);

    // ── ADD these if missing ──────────────────────────────────
    List<User> getAllUsers();

    Optional<User> findById(Long id);

    void deleteUser(Long id);


    boolean existsByEmail(String email);
User saveUser(User user); 
}