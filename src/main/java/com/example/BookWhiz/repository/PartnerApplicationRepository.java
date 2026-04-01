package com.example.BookWhiz.repository;

import com.example.BookWhiz.model.partner.PartnerApplication;
import com.example.BookWhiz.model.partner.PartnerApplication.AppStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnerApplicationRepository
        extends JpaRepository<PartnerApplication, Long> {

    List<PartnerApplication> findAllByOrderByCreatedAtDesc();
    List<PartnerApplication> findByStatusOrderByCreatedAtDesc(AppStatus status);
    Optional<PartnerApplication> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByStatus(AppStatus status);
}