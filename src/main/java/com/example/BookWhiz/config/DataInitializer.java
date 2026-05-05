package com.example.BookWhiz.config;

import com.example.BookWhiz.model.user.Role;
import com.example.BookWhiz.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {

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
    }
}

