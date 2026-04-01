package com.example.BookWhiz.model.permission;

import jakarta.persistence.*;

/**
 * Stores feature-level permissions per role.
 *
 * Each row = one permission toggle.
 * SuperAdmin can flip `enabled` true/false per role per permission.
 *
 * Table: role_permissions
 * Unique constraint: (role, permissionKey)
 */
@Entity
@Table(name = "role_permissions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"role", "permission_key"}))
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String role;            // "ADMIN" | "MANAGER" | "USER"

    @Column(name = "permission_key", nullable = false)
    private String permissionKey;   // e.g. "USER_VIEW", "USER_CREATE"

    @Column(nullable = false)
    private boolean enabled = true;

    public RolePermission() {}
    public RolePermission(String role, String permissionKey, boolean enabled) {
        this.role          = role;
        this.permissionKey = permissionKey;
        this.enabled       = enabled;
    }

    public Long    getId()                          { return id; }
    public String  getRole()                        { return role; }
    public void    setRole(String role)             { this.role = role; }
    public String  getPermissionKey()               { return permissionKey; }
    public void    setPermissionKey(String k)       { this.permissionKey = k; }
    public boolean isEnabled()                      { return enabled; }
    public void    setEnabled(boolean enabled)      { this.enabled = enabled; }
}