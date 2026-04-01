package com.example.BookWhiz.service.user;

import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── GET all ───────────────────────────────────────────────
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ── GET by id ─────────────────────────────────────────────
    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // ── CREATE ────────────────────────────────────────────────
    @Override
    public User createUser(User user) {
        // Hash the password before saving
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    // ── UPDATE ────────────────────────────────────────────────
    @Override
    public Optional<User> updateUser(Long id, User updated) {
        return userRepository.findById(id).map(existing -> {
            if (updated.getName()  != null) existing.setName(updated.getName());
            if (updated.getEmail() != null) existing.setEmail(updated.getEmail());
            if (updated.getRoles()  != null) existing.setRoles(updated.getRoles());

            // Only update password if a new one was provided
            if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(updated.getPassword()));
            }

            return userRepository.save(existing);
        });
    }

    // ── DELETE ────────────────────────────────────────────────
    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
}

@Override
public User saveUser(User user) {
    return userRepository.save(user);
}
}