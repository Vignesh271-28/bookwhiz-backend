package com.example.BookWhiz.repository;

import com.example.BookWhiz.model.permission.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    List<UserPermission>     findByUserId(Long userId);
    Optional<UserPermission> findByUserIdAndPermissionKey(Long userId, String permissionKey);
    void                     deleteByUserIdAndPermissionKey(Long userId, String permissionKey);
    void                     deleteByUserId(Long userId);
}