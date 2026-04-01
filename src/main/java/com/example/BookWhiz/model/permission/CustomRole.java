package com.example.BookWhiz.model.permission;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a custom role created by SuperAdmin.
 * e.g. "Supervisor", "Content Manager", "Auditor"
 *
 * baseSpringRole: the Spring Security role users with this custom role get.
 *   "ADMIN" → can access /api/admin/** endpoints
 *   "MANAGER" → can access /api/manager/** endpoints
 *   "USER" → basic user access
 */
@Entity
@Table(name = "custom_roles",
       uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class CustomRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;           // internal key, e.g. "SUPERVISOR"

    @Column(name = "display_name", nullable = false)
    private String displayName;    // shown in UI, e.g. "Supervisor"

    @Column
    private String description;

    @Column
    private String color;          // hex color, e.g. "#8b5cf6"

    @Column
    private String icon;           // emoji, e.g. "🎯"

    @Column(name = "base_spring_role", nullable = false)
    private String baseSpringRole; // "ADMIN" | "MANAGER" | "USER"

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public CustomRole() {}
    public CustomRole(String name, String displayName, String description,
                      String color, String icon, String baseSpringRole) {
        this.name           = name;
        this.displayName    = displayName;
        this.description    = description;
        this.color          = color;
        this.icon           = icon;
        this.baseSpringRole = baseSpringRole;
    }

    public Long          getId()                             { return id; }
    public String        getName()                           { return name; }
    public void          setName(String name)                { this.name = name; }
    public String        getDisplayName()                    { return displayName; }
    public void          setDisplayName(String displayName)  { this.displayName = displayName; }
    public String        getDescription()                    { return description; }
    public void          setDescription(String d)            { this.description = d; }
    public String        getColor()                          { return color; }
    public void          setColor(String color)              { this.color = color; }
    public String        getIcon()                           { return icon; }
    public void          setIcon(String icon)                { this.icon = icon; }
    public String        getBaseSpringRole()                 { return baseSpringRole; }
    public void          setBaseSpringRole(String r)         { this.baseSpringRole = r; }
    public LocalDateTime getCreatedAt()                      { return createdAt; }
}