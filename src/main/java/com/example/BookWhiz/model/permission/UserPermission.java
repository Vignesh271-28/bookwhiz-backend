package com.example.BookWhiz.model.permission;

import jakarta.persistence.*;

/**
 * Per-user permission override.
 * Overrides take priority over role-level defaults.
 * Table: user_permissions
 */
@Entity
@Table(name = "user_permissions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "permission_key"}))
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "permission_key", nullable = false)
    private String permissionKey;

    @Column(nullable = false)
    private boolean enabled = true;

    public UserPermission() {}
    public UserPermission(Long userId, String permissionKey, boolean enabled) {
        this.userId        = userId;
        this.permissionKey = permissionKey;
        this.enabled       = enabled;
    }

    public Long    getId()                     { return id; }
    public Long    getUserId()                 { return userId; }
    public void    setUserId(Long userId)      { this.userId = userId; }
    public String  getPermissionKey()          { return permissionKey; }
    public void    setPermissionKey(String k)  { this.permissionKey = k; }
    public boolean isEnabled()                 { return enabled; }
    public void    setEnabled(boolean enabled) { this.enabled = enabled; }
}