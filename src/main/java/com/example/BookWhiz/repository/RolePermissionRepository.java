package com.example.BookWhiz.repository;

import com.example.BookWhiz.model.permission.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    Optional<RolePermission> findByRoleAndPermissionKey(String role, String permissionKey);
    List<RolePermission>     findByRole(String role);
}