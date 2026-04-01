package com.example.BookWhiz.controller.admin;

import com.example.BookWhiz.model.user.Role;
import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.repository.RoleRepository;
import com.example.BookWhiz.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin
public class AdminUserController {

    private final UserService     userService;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserService     userService,
                               RoleRepository  roleRepository,
                               PasswordEncoder passwordEncoder) {
        this.userService     = userService;
        this.roleRepository  = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        String name     = body.get("name");
        String email    = body.get("email");
        String password = body.get("password");
        String roleName = body.getOrDefault("role", "USER").toUpperCase();

        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().body("Name is required");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body("Email is required");
        if (password == null || password.isBlank())
            return ResponseEntity.badRequest().body("Password is required");

        Role role = findRole(roleName);
        if (role == null)
            return ResponseEntity.badRequest().body("Unknown role: " + roleName);

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(new java.util.HashSet<>(java.util.List.of(role)));

        return ResponseEntity.ok(userService.saveUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        return userService.findById(id).map(existing -> {

            if (body.containsKey("name")  && !body.get("name").isBlank())
                existing.setName(body.get("name"));
            if (body.containsKey("email") && !body.get("email").isBlank())
                existing.setEmail(body.get("email"));
            if (body.containsKey("password") && !body.get("password").isBlank())
                existing.setPassword(passwordEncoder.encode(body.get("password")));

            if (body.containsKey("role") && !body.get("role").isBlank()) {
                Role role = findRole(body.get("role").toUpperCase());
                if (role != null) existing.setRoles(new java.util.HashSet<>(java.util.List.of(role)));
            }

            return ResponseEntity.ok(userService.saveUser(existing));

        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private Role findRole(String roleName) {
        return roleRepository.findByName(roleName)
                .or(() -> roleRepository.findByName("ROLE_" + roleName))
                .orElse(null);
    }
}