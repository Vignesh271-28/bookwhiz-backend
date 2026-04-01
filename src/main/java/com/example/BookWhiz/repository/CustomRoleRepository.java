package com.example.BookWhiz.repository;

import com.example.BookWhiz.model.permission.CustomRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomRoleRepository extends JpaRepository<CustomRole, Long> {
    Optional<CustomRole> findByName(String name);
    boolean              existsByName(String name);
}