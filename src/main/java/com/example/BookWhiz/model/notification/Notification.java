package com.example.BookWhiz.model.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    public enum NotifType {
        REQUEST_SUBMITTED,   // ADMIN/MANAGER submitted a creation request → notify SUPER_ADMIN
        REQUEST_APPROVED,    // SUPER_ADMIN approved a request → notify requester
        REQUEST_REJECTED     // SUPER_ADMIN rejected a request → notify requester
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NotifType type;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    // Who this notification is FOR (email)
    private String recipientEmail;

    // Role-based broadcast (e.g. "SUPER_ADMIN" = all super admins)
    private String recipientRole;

    @Column(name = "is_read")
    private boolean read = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Getters & Setters ──────────────────────────────────────
    public Long          getId()                    { return id; }
    public void          setId(Long id)             { this.id = id; }

    public NotifType     getType()                  { return type; }
    public void          setType(NotifType t)       { this.type = t; }

    public String        getTitle()                 { return title; }
    public void          setTitle(String t)         { this.title = t; }

    public String        getMessage()               { return message; }
    public void          setMessage(String m)       { this.message = m; }

    public String        getRecipientEmail()        { return recipientEmail; }
    public void          setRecipientEmail(String e){ this.recipientEmail = e; }

    public String        getRecipientRole()         { return recipientRole; }
    public void          setRecipientRole(String r) { this.recipientRole = r; }

    public boolean       isRead()                   { return read; }
    public void          setRead(boolean r)         { this.read = r; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void          setCreatedAt(LocalDateTime t){ this.createdAt = t; }
}