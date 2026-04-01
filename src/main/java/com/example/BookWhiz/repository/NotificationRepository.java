package com.example.BookWhiz.repository;

import com.example.BookWhiz.model.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // For a specific user (personal notifications)
    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String email);

    // For a role-wide broadcast (e.g. all SUPER_ADMINs)
    List<Notification> findByRecipientRoleOrderByCreatedAtDesc(String role);

    // Combined: personal + role-based for a user
    @Query("""
        SELECT n FROM Notification n
        WHERE n.recipientEmail = :email OR n.recipientRole = :role
        ORDER BY n.createdAt DESC
        """)
    List<Notification> findForUser(String email, String role);

    // Unread count
    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE (n.recipientEmail = :email OR n.recipientRole = :role)
        AND n.read = false
        """)
    long countUnreadForUser(String email, String role);

    // Mark all as read
    @Modifying
    @Transactional
    @Query("""
        UPDATE Notification n SET n.read = true
        WHERE n.recipientEmail = :email OR n.recipientRole = :role
        """)
    void markAllReadForUser(String email, String role);
}