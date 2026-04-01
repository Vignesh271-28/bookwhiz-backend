package com.example.BookWhiz.controller.superadmin;

import com.example.BookWhiz.model.permission.RolePermission;
import com.example.BookWhiz.model.permission.UserPermission;
import com.example.BookWhiz.repository.RolePermissionRepository;
import com.example.BookWhiz.repository.UserPermissionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class RolePermissionController {

    private final RolePermissionRepository roleRepo;
    private final UserPermissionRepository userRepo;
    private final EntityManager            em;

    public RolePermissionController(RolePermissionRepository roleRepo,
                                    UserPermissionRepository userRepo,
                                    EntityManager em) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
        this.em       = em;
    }

    // ── All permission definitions ──────────────────────────
    static final List<Map<String, String>> ALL_PERMISSIONS = List.of(
        Map.of("key","USER_VIEW",         "label","View Users",          "category","Users"),
        Map.of("key","USER_CREATE",        "label","Add Users",           "category","Users"),
        Map.of("key","USER_EDIT",          "label","Edit Users",          "category","Users"),
        Map.of("key","USER_DELETE",        "label","Delete Users",        "category","Users"),
        Map.of("key","MOVIE_VIEW",         "label","View Movies",         "category","Movies"),
        Map.of("key","MOVIE_CREATE",       "label","Add Movies",          "category","Movies"),
        Map.of("key","MOVIE_EDIT",         "label","Edit Movies",         "category","Movies"),
        Map.of("key","MOVIE_DELETE",       "label","Delete Movies",       "category","Movies"),
        Map.of("key","SHOW_VIEW",          "label","View Shows",          "category","Shows"),
        Map.of("key","SHOW_CREATE",        "label","Add Shows",           "category","Shows"),
        Map.of("key","SHOW_EDIT",          "label","Edit Shows",          "category","Shows"),
        Map.of("key","SHOW_DELETE",        "label","Delete Shows",        "category","Shows"),
        Map.of("key","VENUE_VIEW",         "label","View Venues",         "category","Venues"),
        Map.of("key","VENUE_CREATE",       "label","Add Venues",          "category","Venues"),
        Map.of("key","VENUE_EDIT",         "label","Edit Venues",         "category","Venues"),
        Map.of("key","VENUE_DELETE",       "label","Delete Venues",       "category","Venues"),
        Map.of("key","BOOKING_VIEW",       "label","View Bookings",       "category","Bookings"),
        Map.of("key","BOOKING_CANCEL",     "label","Cancel Bookings",     "category","Bookings"),
        Map.of("key","BOOKING_CONFIRM",    "label","Confirm Bookings",    "category","Bookings"),
        Map.of("key","STATS_VIEW",         "label","View Stats/Revenue",  "category","Reports"),
        Map.of("key","ANALYTICS_VIEW",     "label","View Analytics",      "category","Reports"),
        Map.of("key","PARTNER_APPROVE",    "label","Approve Partners",    "category","Partners"),
        Map.of("key","ADMIN_DASHBOARD",    "label","Admin Dashboard",     "category","Dashboards"),
        Map.of("key","MANAGER_DASHBOARD",  "label","Manager Dashboard",   "category","Dashboards"),
        Map.of("key","BOOKING_ANALYTICS",  "label","Booking Analytics",   "category","Analytics"),
        Map.of("key","LIVE_ANALYTICS",     "label","Live Activity",       "category","Analytics"),
        Map.of("key","REVENUE_ANALYTICS",  "label","Revenue Analytics",   "category","Analytics"),
        Map.of("key","USER_ANALYTICS",     "label","User Analytics",      "category","Analytics"),
        Map.of("key","APPROVAL_VIEW",      "label","View Approvals",      "category","Approvals"),
        Map.of("key","APPROVAL_MANAGE",    "label","Manage Approvals",    "category","Approvals"),
        Map.of("key","PERMISSION_MANAGE",  "label","Manage Permissions",  "category","Permissions")
    );

    static final List<String> ROLES = List.of("ADMIN", "MANAGER", "USER");

    static final Map<String, Set<String>> DEFAULTS = Map.of(
        "ADMIN", new HashSet<>(List.of(
            "USER_VIEW","USER_CREATE","USER_EDIT","USER_DELETE",
            "MOVIE_VIEW","MOVIE_CREATE","MOVIE_EDIT","MOVIE_DELETE",
            "SHOW_VIEW","SHOW_CREATE","SHOW_EDIT","SHOW_DELETE",
            "VENUE_VIEW","VENUE_CREATE","VENUE_EDIT","VENUE_DELETE",
            "BOOKING_VIEW","BOOKING_CANCEL","BOOKING_CONFIRM",
            "STATS_VIEW","ANALYTICS_VIEW","ADMIN_DASHBOARD",
            "BOOKING_ANALYTICS","LIVE_ANALYTICS","REVENUE_ANALYTICS",
            "USER_ANALYTICS","APPROVAL_VIEW","APPROVAL_MANAGE"
        )),
        "MANAGER", new HashSet<>(List.of(
            "VENUE_VIEW","VENUE_CREATE","VENUE_EDIT","VENUE_DELETE",
            "SHOW_VIEW","SHOW_CREATE","SHOW_EDIT",
            "BOOKING_VIEW","BOOKING_CANCEL","ANALYTICS_VIEW",
            "MANAGER_DASHBOARD","BOOKING_ANALYTICS","REVENUE_ANALYTICS"
        )),
        "USER", new HashSet<>(List.of(
            "BOOKING_VIEW","BOOKING_CANCEL","MOVIE_VIEW","SHOW_VIEW"
        ))
    );

    @PostConstruct
    public void seedDefaults() {
        if (roleRepo.count() > 0) return;
        List<RolePermission> toSave = new ArrayList<>();
        for (String role : ROLES) {
            Set<String> enabled = DEFAULTS.getOrDefault(role, Set.of());
            for (Map<String, String> perm : ALL_PERMISSIONS) {
                String key = perm.get("key");
                toSave.add(new RolePermission(role, key, enabled.contains(key)));
            }
        }
        roleRepo.saveAll(toSave);
    }

    // ════════════════════════════════════════════════════
    // ROLE-LEVEL ENDPOINTS (SuperAdmin only)
    // ════════════════════════════════════════════════════

    @GetMapping("/api/superadmin/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAll() {
        List<RolePermission> saved = roleRepo.findAll();
        Map<String, Map<String, Boolean>> byRole = new LinkedHashMap<>();
        for (String role : ROLES) byRole.put(role, new LinkedHashMap<>());
        for (RolePermission rp : saved)
            if (byRole.containsKey(rp.getRole()))
                byRole.get(rp.getRole()).put(rp.getPermissionKey(), rp.isEnabled());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("permissions", byRole);
        resp.put("definitions", ALL_PERMISSIONS);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/api/superadmin/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> toggle(@RequestBody Map<String, Object> body) {
        String  role    = (String) body.get("role");
        String  key     = (String) body.get("key");
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        if (role == null || key == null)
            return ResponseEntity.badRequest().body("role and key are required");
        RolePermission rp = roleRepo.findByRoleAndPermissionKey(role, key)
                .orElse(new RolePermission(role, key, enabled));
        rp.setEnabled(enabled);
        roleRepo.save(rp);
        return ResponseEntity.ok(Map.of("role", role, "key", key, "enabled", enabled));
    }

    // ════════════════════════════════════════════════════
    // USER-LEVEL ENDPOINTS (SuperAdmin only)
    // ════════════════════════════════════════════════════

    @GetMapping("/api/superadmin/permissions/users/{role}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        String clean      = role.toUpperCase().replaceAll("^ROLE_", "");
        String withPrefix = "ROLE_" + clean;

        List<Object[]> rows = em.createNativeQuery(
            "SELECT u.id, u.name, u.email " +
            "FROM users u " +
            "JOIN user_roles ur ON ur.user_id = u.id " +
            "JOIN roles r ON r.id = ur.role_id " +
            "WHERE UPPER(r.name) = :withPrefix " +
            "   OR UPPER(r.name) = :clean " +
            "ORDER BY u.name ASC"
        )
        .setParameter("withPrefix", withPrefix)
        .setParameter("clean", clean)
        .getResultList();

        List<Long> allUserIds = rows.stream()
            .map(r -> ((Number) r[0]).longValue())
            .collect(Collectors.toList());

        Set<Long> usersWithOverrides = allUserIds.isEmpty() ? Set.of()
            : userRepo.findAll().stream()
                .filter(up -> allUserIds.contains(up.getUserId()))
                .map(UserPermission::getUserId)
                .collect(Collectors.toSet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            long uid = ((Number) r[0]).longValue();
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id",           uid);
            user.put("name",         r[1] != null ? r[1].toString() : "");
            user.put("email",        r[2] != null ? r[2].toString() : "");
            user.put("hasOverrides", usersWithOverrides.contains(uid));
            result.add(user);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/superadmin/permissions/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getUserPermissions(@PathVariable Long userId) {
        List<?> roleRows = em.createNativeQuery(
            "SELECT UPPER(REPLACE(UPPER(r.name), 'ROLE_', '')) AS roleName " +
            "FROM roles r " +
            "JOIN user_roles ur ON ur.role_id = r.id " +
            "WHERE ur.user_id = :uid"
        ).setParameter("uid", userId).getResultList();

        if (roleRows.isEmpty()) return ResponseEntity.notFound().build();

        String userRole = roleRows.get(0).toString();
        if (!ROLES.contains(userRole))
            return ResponseEntity.badRequest().body("Role " + userRole + " has no overrides");

        Map<String, Boolean> roleDefaults = new LinkedHashMap<>();
        for (RolePermission rp : roleRepo.findAll())
            if (rp.getRole().equals(userRole))
                roleDefaults.put(rp.getPermissionKey(), rp.isEnabled());

        Map<String, Boolean> overrides = new LinkedHashMap<>();
        for (UserPermission up : userRepo.findByUserId(userId))
            overrides.put(up.getPermissionKey(), up.isEnabled());

        Map<String, Boolean> merged = new LinkedHashMap<>(roleDefaults);
        merged.putAll(overrides);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("permissions", merged);
        resp.put("overrides",   overrides);
        resp.put("role",        userRole);
        resp.put("definitions", ALL_PERMISSIONS);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/api/superadmin/permissions/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> setUserPermission(@PathVariable Long userId,
                                                @RequestBody Map<String, Object> body) {
        String  key     = (String) body.get("key");
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        if (key == null) return ResponseEntity.badRequest().body("key is required");
        UserPermission up = userRepo.findByUserIdAndPermissionKey(userId, key)
                .orElse(new UserPermission(userId, key, enabled));
        up.setEnabled(enabled);
        userRepo.save(up);
        return ResponseEntity.ok(Map.of("userId", userId, "key", key, "enabled", enabled));
    }

    @DeleteMapping("/api/superadmin/permissions/user/{userId}/{key}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> resetOneUserPermission(@PathVariable Long userId,
                                                     @PathVariable String key) {
        userRepo.deleteByUserIdAndPermissionKey(userId, key);
        return ResponseEntity.ok(Map.of("message", "Override removed"));
    }

    @DeleteMapping("/api/superadmin/permissions/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> resetAllUserPermissions(@PathVariable Long userId) {
        userRepo.deleteByUserId(userId);
        return ResponseEntity.ok(Map.of("message", "All overrides cleared"));
    }

    // ════════════════════════════════════════════════════
    // PUBLIC ENDPOINTS
    // ════════════════════════════════════════════════════

    @GetMapping("/api/permissions/public")
    public ResponseEntity<?> publicPermissions() {
        List<RolePermission> all = roleRepo.findAll();
        Map<String, Map<String, Boolean>> result = new LinkedHashMap<>();
        for (String role : ROLES) result.put(role, new LinkedHashMap<>());
        for (RolePermission rp : all)
            if (result.containsKey(rp.getRole()))
                result.get(rp.getRole()).put(rp.getPermissionKey(), rp.isEnabled());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/permissions/user/{userId}")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> publicUserPermissions(@PathVariable Long userId) {
        List<?> roleRows = em.createNativeQuery(
            "SELECT UPPER(REPLACE(UPPER(r.name), 'ROLE_', '')) AS roleName " +
            "FROM roles r " +
            "JOIN user_roles ur ON ur.role_id = r.id " +
            "WHERE ur.user_id = :uid"
        ).setParameter("uid", userId).getResultList();

        if (roleRows.isEmpty()) return ResponseEntity.ok(Map.of());

        String userRole = roleRows.get(0).toString();

        Map<String, Boolean> merged = new LinkedHashMap<>();
        for (RolePermission rp : roleRepo.findAll())
            if (rp.getRole().equals(userRole))
                merged.put(rp.getPermissionKey(), rp.isEnabled());

        for (UserPermission up : userRepo.findByUserId(userId))
            merged.put(up.getPermissionKey(), up.isEnabled());

        return ResponseEntity.ok(merged);
    }

    /**
     * GET /api/permissions/me
     * Returns merged permissions for the currently authenticated user.
     * Uses JWT session — no userId in token needed.
     */
    @GetMapping("/api/permissions/me")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> myPermissions(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.ok(Map.of());

        // 1. Find user ID by email
        List<?> idRows = em.createNativeQuery(
            "SELECT id FROM users WHERE email = :email")
            .setParameter("email", userDetails.getUsername())
            .getResultList();
        if (idRows.isEmpty()) return ResponseEntity.ok(Map.of());
        Long userId = ((Number) idRows.get(0)).longValue();

        // 2. Check if user has a custom role assignment
        List<?> customRoleRows = em.createNativeQuery(
            "SELECT cr.name " +
            "FROM custom_roles cr " +
            "JOIN user_custom_roles ucr ON ucr.custom_role_id = cr.id " +
            "WHERE ucr.user_id = :uid")
            .setParameter("uid", userId)
            .getResultList();

        // 3. Determine role name to use for permissions
        String effectiveRole = null;
        if (!customRoleRows.isEmpty()) {
            effectiveRole = customRoleRows.get(0).toString();
        }

        // 4. Build merged permissions
        Map<String, Object> merged = new LinkedHashMap<>();

        if (effectiveRole != null) {
            // Load custom role permissions
            for (RolePermission rp : roleRepo.findAll())
                if (rp.getRole().equals(effectiveRole))
                    merged.put(rp.getPermissionKey(), rp.isEnabled());
        } else {
            // Load spring role permissions
            List<?> roleRows = em.createNativeQuery(
                "SELECT UPPER(REPLACE(UPPER(r.name), 'ROLE_', '')) " +
                "FROM roles r JOIN user_roles ur ON ur.role_id = r.id " +
                "WHERE ur.user_id = :uid")
                .setParameter("uid", userId)
                .getResultList();
            if (!roleRows.isEmpty()) {
                String userRole = roleRows.get(0).toString();
                for (RolePermission rp : roleRepo.findAll())
                    if (rp.getRole().equals(userRole))
                        merged.put(rp.getPermissionKey(), rp.isEnabled());
            }
        }

        // 5. Apply user-level overrides on top
        for (com.example.BookWhiz.model.permission.UserPermission up : userRepo.findByUserId(userId))
            merged.put(up.getPermissionKey(), up.isEnabled());

        // 6. Include custom role metadata for frontend display
        if (effectiveRole != null) {
            // Fetch the display name for the custom role
            List<?> displayRows = em.createNativeQuery(
                "SELECT display_name, color, icon FROM custom_roles WHERE name = :name")
                .setParameter("name", effectiveRole)
                .getResultList();
            if (!displayRows.isEmpty()) {
                Object[] dr = (Object[]) displayRows.get(0);
                merged.put("__customRoleName",  dr[0] != null ? dr[0].toString() : effectiveRole);
                merged.put("__customRoleColor", dr[1] != null ? dr[1].toString() : "#8b5cf6");
                merged.put("__customRoleIcon",  dr[2] != null ? dr[2].toString() : "🎭");
            }
        }

        return ResponseEntity.ok(merged);
    }

    // ════════════════════════════════════════════════════
    // ADMIN-LEVEL ENDPOINTS (Admin manages Manager/User)
    // ════════════════════════════════════════════════════

    @GetMapping("/api/admin/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminGetPermissions() {
        List<RolePermission> saved = roleRepo.findAll();
        Map<String, Map<String, Boolean>> byRole = new LinkedHashMap<>();
        byRole.put("MANAGER", new LinkedHashMap<>());
        byRole.put("USER",    new LinkedHashMap<>());
        for (RolePermission rp : saved) {
            if ("MANAGER".equals(rp.getRole()))
                byRole.get("MANAGER").put(rp.getPermissionKey(), rp.isEnabled());
            else if ("USER".equals(rp.getRole()))
                byRole.get("USER").put(rp.getPermissionKey(), rp.isEnabled());
        }
        List<Map<String, String>> managerDefs = ALL_PERMISSIONS.stream()
            .filter(p -> !p.get("category").equals("Permissions"))
            .collect(Collectors.toList());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("permissions", byRole);
        resp.put("definitions", managerDefs);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/api/admin/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminToggle(@RequestBody Map<String, Object> body) {
        String  role    = (String) body.get("role");
        String  key     = (String) body.get("key");
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        if (!List.of("MANAGER", "USER").contains(role))
            return ResponseEntity.status(403).body("Admin can only manage MANAGER/USER permissions");
        RolePermission rp = roleRepo.findByRoleAndPermissionKey(role, key)
                .orElse(new RolePermission(role, key, enabled));
        rp.setEnabled(enabled);
        roleRepo.save(rp);
        return ResponseEntity.ok(Map.of("role", role, "key", key, "enabled", enabled));
    }

    // ════════════════════════════════════════════════════
    // ADMIN USER-LEVEL PERMISSION ENDPOINTS
    // Admin can customize individual Manager/User permissions
    // ════════════════════════════════════════════════════

    /**
     * GET /api/admin/permissions/users/{role}
     * Admin fetches list of users with MANAGER or USER role.
     */
    @GetMapping("/api/admin/permissions/users/{role}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> adminGetUsersByRole(@PathVariable String role) {
        String clean      = role.toUpperCase().replaceAll("^ROLE_", "");
        // Admin can only manage MANAGER and USER roles
        if (!List.of("MANAGER", "USER").contains(clean))
            return ResponseEntity.status(403).body("Admin can only manage MANAGER/USER roles");
        String withPrefix = "ROLE_" + clean;

        List<Object[]> rows = em.createNativeQuery(
            "SELECT u.id, u.name, u.email " +
            "FROM users u " +
            "JOIN user_roles ur ON ur.user_id = u.id " +
            "JOIN roles r ON r.id = ur.role_id " +
            "WHERE UPPER(r.name) = :withPrefix " +
            "   OR UPPER(r.name) = :clean " +
            "ORDER BY u.name ASC"
        )
        .setParameter("withPrefix", withPrefix)
        .setParameter("clean", clean)
        .getResultList();

        List<Long> allUserIds = rows.stream()
            .map(r -> ((Number) r[0]).longValue())
            .collect(Collectors.toList());

        Set<Long> usersWithOverrides = allUserIds.isEmpty() ? Set.of()
            : userRepo.findAll().stream()
                .filter(up -> allUserIds.contains(up.getUserId()))
                .map(UserPermission::getUserId)
                .collect(Collectors.toSet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            long uid = ((Number) r[0]).longValue();
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id",           uid);
            user.put("name",         r[1] != null ? r[1].toString() : "");
            user.put("email",        r[2] != null ? r[2].toString() : "");
            user.put("hasOverrides", usersWithOverrides.contains(uid));
            result.add(user);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/permissions/user/{userId}
     * Admin fetches merged permissions for a specific Manager or User.
     */
    @GetMapping("/api/admin/permissions/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> adminGetUserPermissions(@PathVariable Long userId) {
        List<?> roleRows = em.createNativeQuery(
            "SELECT UPPER(REPLACE(UPPER(r.name), 'ROLE_', '')) " +
            "FROM roles r " +
            "JOIN user_roles ur ON ur.role_id = r.id " +
            "WHERE ur.user_id = :uid"
        ).setParameter("uid", userId).getResultList();

        if (roleRows.isEmpty()) return ResponseEntity.notFound().build();
        String userRole = roleRows.get(0).toString();

        // Admin can only manage MANAGER and USER
        if (!List.of("MANAGER", "USER").contains(userRole))
            return ResponseEntity.status(403).body("Admin can only manage MANAGER/USER permissions");

        Map<String, Boolean> roleDefaults = new LinkedHashMap<>();
        for (RolePermission rp : roleRepo.findAll())
            if (rp.getRole().equals(userRole))
                roleDefaults.put(rp.getPermissionKey(), rp.isEnabled());

        Map<String, Boolean> overrides = new LinkedHashMap<>();
        for (UserPermission up : userRepo.findByUserId(userId))
            overrides.put(up.getPermissionKey(), up.isEnabled());

        Map<String, Boolean> merged = new LinkedHashMap<>(roleDefaults);
        merged.putAll(overrides);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("permissions", merged);
        resp.put("overrides",   overrides);
        resp.put("role",        userRole);
        resp.put("definitions", ALL_PERMISSIONS);
        return ResponseEntity.ok(resp);
    }

    /**
     * PUT /api/admin/permissions/user/{userId}
     * Admin sets a permission override for a specific Manager or User.
     */
    @PutMapping("/api/admin/permissions/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> adminSetUserPermission(@PathVariable Long userId,
                                                     @RequestBody Map<String, Object> body) {
        return setUserPermission(userId, body);
    }

    /**
     * DELETE /api/admin/permissions/user/{userId}/{key}
     * Admin resets one override for a Manager/User.
     */
    @DeleteMapping("/api/admin/permissions/user/{userId}/{key}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> adminResetOneUserPermission(@PathVariable Long userId,
                                                          @PathVariable String key) {
        return resetOneUserPermission(userId, key);
    }

    /**
     * DELETE /api/admin/permissions/user/{userId}
     * Admin resets all overrides for a Manager/User.
     */
    @DeleteMapping("/api/admin/permissions/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> adminResetAllUserPermissions(@PathVariable Long userId) {
        return resetAllUserPermissions(userId);
    }


    /**
     * GET /api/roles/me
     * Returns the custom role assigned to the current user (if any).
     * Used by frontend to display the correct role name.
     */
    @GetMapping("/api/roles/me")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> myCustomRole(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.ok(Map.of());

        List<?> idRows = em.createNativeQuery(
            "SELECT id FROM users WHERE email = :email")
            .setParameter("email", userDetails.getUsername())
            .getResultList();

        if (idRows.isEmpty()) return ResponseEntity.ok(Map.of());
        Long userId = ((Number) idRows.get(0)).longValue();

        // Check if user has a custom role
        List<?> customRoleRows = em.createNativeQuery(
            "SELECT cr.id, cr.name, cr.display_name, cr.color, cr.icon, cr.base_spring_role " +
            "FROM custom_roles cr " +
            "JOIN user_custom_roles ucr ON ucr.custom_role_id = cr.id " +
            "WHERE ucr.user_id = :uid")
            .setParameter("uid", userId)
            .getResultList();

        if (customRoleRows.isEmpty()) return ResponseEntity.ok(Map.of());

        Object[] row = (Object[]) customRoleRows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id",             ((Number) row[0]).longValue());
        result.put("name",           row[1] != null ? row[1].toString() : "");
        result.put("displayName",    row[2] != null ? row[2].toString() : "");
        result.put("color",          row[3] != null ? row[3].toString() : "#6b7280");
        result.put("icon",           row[4] != null ? row[4].toString() : "🎭");
        result.put("baseSpringRole", row[5] != null ? row[5].toString() : "USER");
        return ResponseEntity.ok(result);
    }

}