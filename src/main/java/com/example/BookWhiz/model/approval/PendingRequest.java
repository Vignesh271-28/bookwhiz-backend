package com.example.BookWhiz.model.approval;

import jakarta.persistence.*;
import java.time.LocalDateTime;
    
@Entity
@Table(name = "pending_requests")
public class PendingRequest {

    public enum RequestType   { MOVIE, VENUE, USER }
    public enum RequestStatus { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private RequestType type;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    // Who submitted this request
    private String requestedByEmail;
    private String requestedByName;

    // Full JSON payload of the object to create (MovieRequest, Venue, etc.)
    @Column(columnDefinition = "TEXT")
    private String payload;

    // Human-readable summary shown in the approval queue
    private String summary;

    private LocalDateTime createdAt  = LocalDateTime.now();
    private LocalDateTime reviewedAt;
    private String        reviewNote; // optional reject reason

    // ── Getters & Setters ────────────────────────────────────
    public Long            getId()                  { return id; }
    public void            setId(Long id)           { this.id = id; }

    public RequestType     getType()                { return type; }
    public void            setType(RequestType t)   { this.type = t; }

    public RequestStatus   getStatus()              { return status; }
    public void            setStatus(RequestStatus s){ this.status = s; }

    public String getRequestedByEmail()             { return requestedByEmail; }
    public void   setRequestedByEmail(String e)     { this.requestedByEmail = e; }

    public String getRequestedByName()              { return requestedByName; }
    public void   setRequestedByName(String n)      { this.requestedByName = n; }

    public String getPayload()                      { return payload; }
    public void   setPayload(String p)              { this.payload = p; }

    public String getSummary()                      { return summary; }
    public void   setSummary(String s)              { this.summary = s; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void          setCreatedAt(LocalDateTime t){ this.createdAt = t; }

    public LocalDateTime getReviewedAt()            { return reviewedAt; }
    public void          setReviewedAt(LocalDateTime t){ this.reviewedAt = t; }

    public String getReviewNote()                   { return reviewNote; }
    public void   setReviewNote(String n)           { this.reviewNote = n; }
}