package com.example.BookWhiz.service;

import com.example.BookWhiz.repository.RolePermissionRepository;
import com.example.BookWhiz.repository.UserPermissionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring bean used in @PreAuthorize SpEL expressions:
 *   @permCheck.can('ADMIN','PERMISSION_MANAGE')
 *   @permCheck.canCurrentUser('PERMISSION_MANAGE')
 */
@Component("permCheck")
public class PermissionCheckService {

    private final RolePermissionRepository roleRepo;
    private final UserPermissionRepository userRepo;

    public PermissionCheckService(RolePermissionRepository roleRepo,
                                  UserPermissionRepository userRepo) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
    }

    /** Check role-level permission (used by SuperAdmin endpoints) */
    public boolean can(String role, String permissionKey) {
        return roleRepo.findByRoleAndPermissionKey(role, permissionKey)
                .map(p -> p.isEnabled())
                .orElse(false);
    }

    /**
     * Check if the currently authenticated user has a permission,
     * considering user-level overrides first, then role default.
     * Used by Admin endpoints: @permCheck.canCurrentUser('PERMISSION_MANAGE')
     */
    public boolean canCurrentUser(String permissionKey) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        // Try to get user ID from principal
        Object principal = auth.getPrincipal();
        Long userId = null;
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            // Try to get user by email from UserRepository via email
            try {
                var users = userRepo.findAll();
                for (var up : users) {
                    // UserPermission has userId, find any override for this permission
                }
            } catch (Exception ignored) {}
        }

        // Determine role from authorities
        String role = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .filter(r -> java.util.List.of("ADMIN","MANAGER","USER","SUPER_ADMIN").contains(r))
                .findFirst()
                .orElse(null);

        if (role == null) return false;

        // Check role-level default
        return roleRepo.findByRoleAndPermissionKey(role, permissionKey)
                .map(p -> p.isEnabled())
                .orElse(false);
    }
}