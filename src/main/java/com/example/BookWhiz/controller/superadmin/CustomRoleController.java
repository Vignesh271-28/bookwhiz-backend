package com.example.BookWhiz.controller.superadmin;

import com.example.BookWhiz.model.permission.CustomRole;
import com.example.BookWhiz.model.permission.RolePermission;
import com.example.BookWhiz.model.permission.UserCustomRole;
import com.example.BookWhiz.repository.CustomRoleRepository;
import com.example.BookWhiz.repository.RolePermissionRepository;
import com.example.BookWhiz.repository.UserCustomRoleRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Endpoints for dynamic custom role management (SuperAdmin only):
 *
 *   GET    /api/superadmin/roles              → list all custom roles with user count
 *   POST   /api/superadmin/roles              → create new custom role
 *   PUT    /api/superadmin/roles/{id}         → update role metadata
 *   DELETE /api/superadmin/roles/{id}         → delete role (unassigns all users)
 *
 *   GET    /api/superadmin/roles/{id}/permissions  → get permissions for role
 *   PUT    /api/superadmin/roles/{id}/permissions  → toggle one permission
 *
 *   GET    /api/superadmin/roles/{id}/users        → users assigned to role
 *   POST   /api/superadmin/roles/{id}/users/{uid}  → assign user to role
 *   DELETE /api/superadmin/roles/{id}/users/{uid}  → remove user from role
 *
 *   GET    /api/superadmin/roles/assignable-users  → users not yet in a custom role
 *
 * Public:
 *   GET    /api/roles/public                  → all custom roles (for frontend display)
 */
@RestController
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CustomRoleController {

    private final CustomRoleRepository     roleRepo;
    private final RolePermissionRepository permRepo;
    private final UserCustomRoleRepository userRoleRepo;
    private final EntityManager            em;

    public CustomRoleController(CustomRoleRepository roleRepo,
                                RolePermissionRepository permRepo,
                                UserCustomRoleRepository userRoleRepo,
                                EntityManager em) {
        this.roleRepo     = roleRepo;
        this.permRepo     = permRepo;
        this.userRoleRepo = userRoleRepo;
        this.em           = em;
    }

    // ── List all custom roles ─────────────────────────────────
    @GetMapping("/api/superadmin/roles")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listRoles() {
        List<CustomRole> roles = roleRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (CustomRole r : roles) {
            int userCount = userRoleRepo.findByCustomRoleId(r.getId()).size();
            int enabledPerms = (int) permRepo.findByRole(r.getName())
                    .stream().filter(RolePermission::isEnabled).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",             r.getId());
            m.put("name",           r.getName());
            m.put("displayName",    r.getDisplayName());
            m.put("description",    r.getDescription());
            m.put("color",          r.getColor());
            m.put("icon",           r.getIcon());
            m.put("baseSpringRole", r.getBaseSpringRole());
            m.put("userCount",      userCount);
            m.put("enabledPerms",   enabledPerms);
            m.put("createdAt",      r.getCreatedAt());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // ── Create new custom role ────────────────────────────────
    @PostMapping("/api/superadmin/roles")
    @Transactional
    public ResponseEntity<?> createRole(@RequestBody Map<String, Object> body) {
        String name        = ((String) body.getOrDefault("name", "")).toUpperCase()
                                .replaceAll("[^A-Z0-9_]", "_");
        String displayName = (String) body.getOrDefault("displayName", name);
        String description = (String) body.getOrDefault("description", "");
        String color       = (String) body.getOrDefault("color", "#6b7280");
        String icon        = (String) body.getOrDefault("icon", "🎭");
        String baseRole    = (String) body.getOrDefault("baseSpringRole", "USER");

        if (name.isBlank())
            return ResponseEntity.badRequest().body("Role name is required");
        if (roleRepo.existsByName(name))
            return ResponseEntity.badRequest().body("Role '" + name + "' already exists");
        if (!List.of("ADMIN","MANAGER","USER").contains(baseRole))
            return ResponseEntity.badRequest().body("baseSpringRole must be ADMIN, MANAGER, or USER");

        CustomRole role = new CustomRole(name, displayName, description, color, icon, baseRole);
        CustomRole saved = roleRepo.save(role);

        // Seed permission rows for this role (all disabled by default)
        List<RolePermission> perms = RolePermissionController.ALL_PERMISSIONS.stream()
            .map(p -> new RolePermission(name, p.get("key"), false))
            .collect(Collectors.toList());
        permRepo.saveAll(perms);

        return ResponseEntity.ok(Map.of("id", saved.getId(), "name", name,
                "message", "Role created successfully"));
    }

    // ── Update role metadata ──────────────────────────────────
    @PutMapping("/api/superadmin/roles/{id}")
    @Transactional
    public ResponseEntity<?> updateRole(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body) {
        return roleRepo.findById(id).map(role -> {
            if (body.containsKey("displayName")) role.setDisplayName((String) body.get("displayName"));
            if (body.containsKey("description")) role.setDescription((String) body.get("description"));
            if (body.containsKey("color"))       role.setColor((String) body.get("color"));
            if (body.containsKey("icon"))        role.setIcon((String) body.get("icon"));
            if (body.containsKey("baseSpringRole")) {
                String base = (String) body.get("baseSpringRole");
                if (List.of("ADMIN","MANAGER","USER").contains(base))
                    role.setBaseSpringRole(base);
            }
            roleRepo.save(role);
            return ResponseEntity.ok(Map.of("message", "Role updated"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Delete custom role ────────────────────────────────────
    @DeleteMapping("/api/superadmin/roles/{id}")
    @Transactional
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        return roleRepo.findById(id).map(role -> {
            // Remove all user assignments
            userRoleRepo.deleteByCustomRoleId(id);
            // Remove all permission rows
            List<RolePermission> perms = permRepo.findByRole(role.getName());
            permRepo.deleteAll(perms);
            roleRepo.delete(role);
            return ResponseEntity.ok(Map.of("message", "Role deleted"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Get permissions for a custom role ─────────────────────
    @GetMapping("/api/superadmin/roles/{id}/permissions")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRolePermissions(@PathVariable Long id) {
        return roleRepo.findById(id).map(role -> {
            Map<String, Boolean> perms = new LinkedHashMap<>();
            permRepo.findByRole(role.getName())
                    .forEach(p -> perms.put(p.getPermissionKey(), p.isEnabled()));
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("permissions", perms);
            resp.put("definitions", RolePermissionController.ALL_PERMISSIONS);
            resp.put("role",        role.getName());
            return ResponseEntity.ok(resp);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Toggle one permission for a custom role ───────────────
    @PutMapping("/api/superadmin/roles/{id}/permissions")
    @Transactional
    public ResponseEntity<?> toggleRolePermission(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> body) {
        return roleRepo.findById(id).map(role -> {
            String  key     = (String) body.get("key");
            boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
            if (key == null)
                return ResponseEntity.badRequest().body("key is required");
            RolePermission rp = permRepo.findByRoleAndPermissionKey(role.getName(), key)
                    .orElse(new RolePermission(role.getName(), key, enabled));
            rp.setEnabled(enabled);
            permRepo.save(rp);
            return ResponseEntity.ok(Map.of("key", key, "enabled", enabled));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── List users assigned to a role ─────────────────────────
    @GetMapping("/api/superadmin/roles/{id}/users")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getRoleUsers(@PathVariable Long id) {
        List<UserCustomRole> assignments = userRoleRepo.findByCustomRoleId(id);
        if (assignments.isEmpty()) return ResponseEntity.ok(List.of());

        List<Long> userIds = assignments.stream()
                .map(UserCustomRole::getUserId).collect(Collectors.toList());

        String inClause = userIds.stream()
                .map(String::valueOf).collect(Collectors.joining(","));
        List<Object[]> rows = em.createNativeQuery(
            "SELECT id, name, email FROM users WHERE id IN (" + inClause + ")")
            .getResultList();

        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",    ((Number) r[0]).longValue());
            m.put("name",  r[1] != null ? r[1].toString() : "");
            m.put("email", r[2] != null ? r[2].toString() : "");
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── Assign user to role ───────────────────────────────────
    @PostMapping("/api/superadmin/roles/{id}/users/{userId}")
    @Transactional
    public ResponseEntity<?> assignUser(@PathVariable Long id, @PathVariable Long userId) {
        if (!roleRepo.existsById(id))
            return ResponseEntity.notFound().build();
        // Remove any existing custom role assignment first
        userRoleRepo.findByUserId(userId).ifPresent(userRoleRepo::delete);
        // Assign new role
        userRoleRepo.save(new UserCustomRole(userId, id));
        return ResponseEntity.ok(Map.of("message", "User assigned to role"));
    }

    // ── Remove user from role ─────────────────────────────────
    @DeleteMapping("/api/superadmin/roles/{id}/users/{userId}")
    @Transactional
    public ResponseEntity<?> removeUser(@PathVariable Long id, @PathVariable Long userId) {
        userRoleRepo.findByUserId(userId).ifPresent(ucr -> {
            if (ucr.getCustomRoleId().equals(id)) userRoleRepo.delete(ucr);
        });
        return ResponseEntity.ok(Map.of("message", "User removed from role"));
    }

    // ── Assignable users (not already in a custom role) ───────
    @GetMapping("/api/superadmin/roles/assignable-users")
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getAssignableUsers() {
        Set<Long> assignedIds = userRoleRepo.findAll().stream()
                .map(UserCustomRole::getUserId).collect(Collectors.toSet());

        List<Object[]> rows = em.createNativeQuery(
            "SELECT u.id, u.name, u.email, " +
            "UPPER(REPLACE(UPPER(r.name),'ROLE_','')) AS roleName " +
            "FROM users u " +
            "JOIN user_roles ur ON ur.user_id = u.id " +
            "JOIN roles r ON r.id = ur.role_id " +
            "WHERE UPPER(r.name) NOT LIKE '%SUPER_ADMIN%' " +
            "ORDER BY u.name ASC")
            .getResultList();

        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            long uid = ((Number) r[0]).longValue();
            m.put("id",         uid);
            m.put("name",       r[1] != null ? r[1].toString() : "");
            m.put("email",      r[2] != null ? r[2].toString() : "");
            m.put("role",       r[3] != null ? r[3].toString() : "");
            m.put("assigned",   assignedIds.contains(uid));
            m.put("customRole", userRoleRepo.findByUserId(uid)
                .map(ucr -> roleRepo.findById(ucr.getCustomRoleId())
                    .map(cr -> Map.of("id", cr.getId(), "name", cr.getName(),
                                      "displayName", cr.getDisplayName()))
                    .orElse(null))
                .orElse(null));
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── Public endpoint (no auth needed) ─────────────────────
    @GetMapping("/api/roles/public")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> publicRoles() {
        List<Map<String, Object>> result = roleRepo.findAll().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          r.getId());
            m.put("name",        r.getName());
            m.put("displayName", r.getDisplayName());
            m.put("color",       r.getColor());
            m.put("icon",        r.getIcon());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}