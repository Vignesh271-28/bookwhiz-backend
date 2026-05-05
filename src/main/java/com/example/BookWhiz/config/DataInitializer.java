package com.example.BookWhiz.config;

import com.example.BookWhiz.model.user.Role;
import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.repository.RoleRepository;
import com.example.BookWhiz.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // 1. Seed Roles
        List<String> roles = List.of(
                "SUPER_ADMIN",
                "ADMIN",
                "MANAGER",
                "USER"
        );

        for (String roleName : roles) {
            roleRepository.findByName(roleName)
                    .orElseGet(() ->
                            roleRepository.save(new Role(roleName))
                    );
        }

        // 2. Seed Super Admin User
        String superAdminEmail = System.getenv("SUPER_ADMIN_EMAIL") != null ? System.getenv("SUPER_ADMIN_EMAIL") : "superadmin@gmail.com";
        String superAdminPassword = System.getenv("SUPER_ADMIN_PASSWORD") != null ? System.getenv("SUPER_ADMIN_PASSWORD") : "superadmin";

        if (!userRepository.existsByEmail(superAdminEmail)) {
            User superAdmin = new User();
            superAdmin.setName("Super Admin");
            superAdmin.setEmail(superAdminEmail);
            superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));

            Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Error: Role SUPER_ADMIN is not found."));

            superAdmin.setRoles(Set.of(superAdminRole));
            userRepository.save(superAdmin);
            System.out.println("Default Super Admin user created: " + superAdminEmail);
        }
    }
}

