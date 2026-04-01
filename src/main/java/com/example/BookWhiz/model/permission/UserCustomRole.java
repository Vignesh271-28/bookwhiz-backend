package com.example.BookWhiz.model.permission;

import jakarta.persistence.*;

/**
 * Assigns a user to a custom role.
 * A user can have only one custom role at a time.
 * Table: user_custom_roles
 */
@Entity
@Table(name = "user_custom_roles",
       uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class UserCustomRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "custom_role_id", nullable = false)
    private Long customRoleId;

    public UserCustomRole() {}
    public UserCustomRole(Long userId, Long customRoleId) {
        this.userId       = userId;
        this.customRoleId = customRoleId;
    }

    public Long getId()                        { return id; }
    public Long getUserId()                    { return userId; }
    public void setUserId(Long userId)         { this.userId = userId; }
    public Long getCustomRoleId()              { return customRoleId; }
    public void setCustomRoleId(Long id)       { this.customRoleId = id; }
}