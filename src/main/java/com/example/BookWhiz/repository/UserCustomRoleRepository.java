package com.example.BookWhiz.repository;

import com.example.BookWhiz.model.permission.UserCustomRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserCustomRoleRepository extends JpaRepository<UserCustomRole, Long> {
    Optional<UserCustomRole> findByUserId(Long userId);
    List<UserCustomRole>     findByCustomRoleId(Long customRoleId);
    void                     deleteByUserId(Long userId);
    void                     deleteByCustomRoleId(Long customRoleId);
}