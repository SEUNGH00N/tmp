package com.example.excelimport.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class RoleAuthorizationService {

    private final JdbcTemplate jdbcTemplate;

    public RoleAuthorizationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<String> getRolesByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(jdbcTemplate.query(
                "select r.code from app_role r " +
                        "join app_user_role ur on ur.role_id = r.id " +
                        "join app_user u on u.id = ur.user_id " +
                        "where u.username = ? and u.active = true and r.active = true " +
                        "order by r.code",
                (rs, n) -> rs.getString("code"),
                username
        ));
    }

    public boolean hasAnyRole(String username, Collection<String> required) {
        if (required == null || required.isEmpty()) {
            return true;
        }
        Set<String> current = getRolesByUsername(username);
        for (String role : required) {
            if (current.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
