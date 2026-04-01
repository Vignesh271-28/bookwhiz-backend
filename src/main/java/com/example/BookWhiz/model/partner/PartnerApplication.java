package com.example.BookWhiz.model.partner;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a theatre owner's request to join as a BookWhiz partner.
 * Table: partner_applications
 * Hibernate will auto-create this table on startup.
 */
@Entity
@Table(name = "partner_applications")
public class PartnerApplication {

    public enum AppStatus { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Contact Info ─────────────────────────────────────────
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    private String password;   // hashed before storing, set during registration

    // ── Theatre Info ─────────────────────────────────────────
    @Column(name = "theatre_name", nullable = false)
    private String theatreName;

    @Column(nullable = false)
    private String city;

    private String state;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    private String pincode;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ── Status ───────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppStatus status = AppStatus.PENDING;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** ID of the User account created when this application was approved */
    @Column(name = "created_user_id")
    private Long createdUserId;

    // ── Generated getters/setters ────────────────────────────
    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }
    public String getName()                      { return name; }
    public void setName(String name)             { this.name = name; }
    public String getEmail()                     { return email; }
    public void setEmail(String email)           { this.email = email; }
    public String getPhone()                     { return phone; }
    public void setPhone(String phone)           { this.phone = phone; }
    public String getPassword()                  { return password; }
    public void setPassword(String password)     { this.password = password; }
    public String getTheatreName()               { return theatreName; }
    public void setTheatreName(String v)         { this.theatreName = v; }
    public String getCity()                      { return city; }
    public void setCity(String city)             { this.city = city; }
    public String getState()                     { return state; }
    public void setState(String state)           { this.state = state; }
    public String getAddress()                   { return address; }
    public void setAddress(String address)       { this.address = address; }
    public String getPincode()                   { return pincode; }
    public void setPincode(String pincode)       { this.pincode = pincode; }
    public String getWebsiteUrl()                { return websiteUrl; }
    public void setWebsiteUrl(String v)          { this.websiteUrl = v; }
    public String getDescription()               { return description; }
    public void setDescription(String v)         { this.description = v; }
    public AppStatus getStatus()                 { return status; }
    public void setStatus(AppStatus status)      { this.status = status; }
    public String getReviewNote()                { return reviewNote; }
    public void setReviewNote(String v)          { this.reviewNote = v; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public void setCreatedAt(LocalDateTime v)    { this.createdAt = v; }
    public LocalDateTime getReviewedAt()         { return reviewedAt; }
    public void setReviewedAt(LocalDateTime v)   { this.reviewedAt = v; }
    public Long getCreatedUserId()               { return createdUserId; }
    public void setCreatedUserId(Long v)         { this.createdUserId = v; }
}