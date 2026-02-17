package com.example.excelimport.service;

import com.example.excelimport.dto.AdminRoleItem;
import com.example.excelimport.dto.AdminUserItem;
import com.example.excelimport.dto.PagedAdminUsersResponse;
import com.example.excelimport.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminUserManagementService {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final ObjectMapper objectMapper;

    public AdminUserManagementService(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedJdbc,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbc = namedJdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PagedAdminUsersResponse getUsers(Pageable pageable) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.max(1, pageable.getPageSize());
        long offset = (long) page * size;
        long total = Objects.requireNonNullElse(
                jdbcTemplate.queryForObject("select count(*) from app_user", Long.class),
                0L
        );

        List<BasicUserRow> rows = namedJdbc.query(
                "select id, username, display_name, active, created_at " +
                        "from app_user order by created_at desc limit :limit offset :offset",
                new MapSqlParameterSource()
                        .addValue("limit", size)
                        .addValue("offset", offset),
                (rs, n) -> new BasicUserRow(
                        (UUID) rs.getObject("id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getBoolean("active"),
                        rs.getTimestamp("created_at").toInstant()
                )
        );

        Map<UUID, List<String>> rolesByUserId = loadRolesByUserIds(rows.stream().map(BasicUserRow::id).toList());
        List<AdminUserItem> items = rows.stream()
                .map(row -> new AdminUserItem(
                        row.id(),
                        row.username(),
                        row.displayName(),
                        row.active(),
                        row.createdAt(),
                        rolesByUserId.getOrDefault(row.id(), List.of())
                ))
                .toList();

        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PagedAdminUsersResponse(items, page, size, total, totalPages);
    }

    @Transactional(readOnly = true)
    public List<AdminRoleItem> getRoles() {
        return jdbcTemplate.query(
                "select r.id, r.code, r.name, r.description, r.active, count(ur.user_id) as user_count " +
                        "from app_role r left join app_user_role ur on ur.role_id = r.id " +
                        "group by r.id, r.code, r.name, r.description, r.active " +
                        "order by r.code",
                (rs, n) -> new AdminRoleItem(
                        (UUID) rs.getObject("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBoolean("active"),
                        rs.getLong("user_count")
                )
        );
    }

    @Transactional
    public AdminUserItem updateUserStatus(UUID userId, boolean active, String actorUsername) {
        Map<String, Object> before = loadUserSnapshot(userId);
        int updated = jdbcTemplate.update("update app_user set active = ? where id = ?", active, userId);
        if (updated == 0) {
            throw notFound("user not found");
        }
        Map<String, Object> after = loadUserSnapshot(userId);
        insertAudit(actorUsername, "APP_USER", userId, "USER_STATUS_UPDATED", before, after);
        return getUser(userId);
    }

    @Transactional
    public AdminUserItem updateUserRoles(UUID userId, List<String> roleCodes, String actorUsername) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw badRequest("roleCodes must not be empty");
        }

        ensureUserExists(userId);
        Set<String> normalized = new LinkedHashSet<>();
        for (String role : roleCodes) {
            if (role != null && !role.isBlank()) {
                normalized.add(role.trim().toUpperCase());
            }
        }
        if (normalized.isEmpty()) {
            throw badRequest("roleCodes must not be empty");
        }

        MapSqlParameterSource params = new MapSqlParameterSource("codes", normalized);
        List<String> validCodes = namedJdbc.query(
                "select code from app_role where active = true and code in (:codes)",
                params,
                (rs, n) -> rs.getString("code")
        );
        if (validCodes.size() != normalized.size()) {
            Set<String> missing = new LinkedHashSet<>(normalized);
            missing.removeAll(validCodes);
            throw badRequest("invalid roleCodes: " + String.join(", ", missing));
        }

        List<String> beforeRoles = getRoleCodesByUserId(userId);
        jdbcTemplate.update("delete from app_user_role where user_id = ?", userId);

        UUID actorId = getUserIdByUsername(actorUsername);
        for (String code : normalized) {
            UUID roleId = jdbcTemplate.queryForObject("select id from app_role where code = ?", UUID.class, code);
            jdbcTemplate.update(
                    "insert into app_user_role (id, user_id, role_id, assigned_by, created_at) values (?, ?, ?, ?, ?)",
                    UUID.randomUUID(), userId, roleId, actorId, Timestamp.from(Instant.now())
            );
        }
        List<String> afterRoles = getRoleCodesByUserId(userId);
        insertAudit(
                actorUsername,
                "APP_USER",
                userId,
                "USER_ROLES_UPDATED",
                Map.of("roles", beforeRoles),
                Map.of("roles", afterRoles)
        );
        return getUser(userId);
    }

    private void ensureUserExists(UUID userId) {
        Long count = jdbcTemplate.queryForObject("select count(*) from app_user where id = ?", Long.class, userId);
        if (count == null || count == 0L) {
            throw notFound("user not found");
        }
    }

    private UUID getUserIdByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return jdbcTemplate.query(
                "select id from app_user where username = ?",
                (rs, n) -> (UUID) rs.getObject("id"),
                username
        ).stream().findFirst().orElse(null);
    }

    private List<String> getRoleCodesByUserId(UUID userId) {
        return jdbcTemplate.query(
                "select r.code from app_user_role ur join app_role r on ur.role_id = r.id where ur.user_id = ? order by r.code",
                (rs, n) -> rs.getString("code"),
                userId
        );
    }

    private AdminUserItem getUser(UUID userId) {
        List<BasicUserRow> users = jdbcTemplate.query(
                "select id, username, display_name, active, created_at from app_user where id = ?",
                (rs, n) -> new BasicUserRow(
                        (UUID) rs.getObject("id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getBoolean("active"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                userId
        );
        if (users.isEmpty()) {
            throw notFound("user not found");
        }
        BasicUserRow user = users.get(0);
        return new AdminUserItem(
                user.id(),
                user.username(),
                user.displayName(),
                user.active(),
                user.createdAt(),
                getRoleCodesByUserId(userId)
        );
    }

    private Map<UUID, List<String>> loadRolesByUserIds(List<UUID> userIds) {
        Map<UUID, List<String>> out = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return out;
        }
        List<UserRoleRow> roleRows = namedJdbc.query(
                "select ur.user_id, r.code " +
                        "from app_user_role ur join app_role r on ur.role_id = r.id " +
                        "where ur.user_id in (:ids) order by r.code",
                new MapSqlParameterSource("ids", userIds),
                (rs, n) -> new UserRoleRow((UUID) rs.getObject("user_id"), rs.getString("code"))
        );
        for (UserRoleRow row : roleRows) {
            out.computeIfAbsent(row.userId(), k -> new ArrayList<>()).add(row.roleCode());
        }
        return out;
    }

    private Map<String, Object> loadUserSnapshot(UUID userId) {
        AdminUserItem user = getUser(userId);
        return Map.of(
                "id", user.id().toString(),
                "username", user.username(),
                "displayName", user.displayName(),
                "active", user.active(),
                "roles", user.roles()
        );
    }

    private void insertAudit(
            String actor,
            String targetType,
            UUID targetId,
            String action,
            Map<String, Object> before,
            Map<String, Object> after
    ) {
        try {
            String beforeJson = objectMapper.writeValueAsString(before);
            String afterJson = objectMapper.writeValueAsString(after);
            jdbcTemplate.update(
                    "insert into audit_log (id, actor, target_type, target_id, action, before_json, after_json, created_at) " +
                            "values (?, ?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb), ?)",
                    UUID.randomUUID(),
                    actor == null ? "system" : actor,
                    targetType,
                    targetId,
                    action,
                    beforeJson,
                    afterJson,
                    Timestamp.from(Instant.now())
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize audit payload", e);
        }
    }

    private ApiException badRequest(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "https://example.com/problems/bad-request",
                "Bad Request",
                detail);
    }

    private ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND,
                "https://example.com/problems/not-found",
                "Not Found",
                detail);
    }

    private record BasicUserRow(
            UUID id,
            String username,
            String displayName,
            boolean active,
            Instant createdAt
    ) {
    }

    private record UserRoleRow(UUID userId, String roleCode) {
    }
}
