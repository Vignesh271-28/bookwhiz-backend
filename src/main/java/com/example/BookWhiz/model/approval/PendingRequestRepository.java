package com.example.BookWhiz.model.approval;

import com.example.BookWhiz.model.approval.PendingRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PendingRequestRepository extends JpaRepository<PendingRequest, Long> {

    List<PendingRequest> findAllByOrderByCreatedAtDesc();

    List<PendingRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    long countByStatus(RequestStatus status);
}