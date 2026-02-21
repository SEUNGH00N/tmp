package com.example.excelimport.userwas.service;

import com.example.excelimport.userwas.dto.UserDbConnectionCreateRequest;
import com.example.excelimport.userwas.dto.UserDbConnectionCreateResponse;
import com.example.excelimport.userwas.dto.UserDbConnectionItemResponse;
import com.example.excelimport.userwas.dto.UserDbConnectionListResponse;
import com.example.excelimport.userwas.dto.UserDbConnectionTestResponse;
import com.example.excelimport.userwas.exception.UserWasException;
import com.example.excelimport.userwas.security.ConnectionSecretCrypto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Service
public class UserDbConnectionService {

    private final JdbcTemplate jdbcTemplate;
    private final ConnectionSecretCrypto connectionSecretCrypto;

    public UserDbConnectionService(JdbcTemplate jdbcTemplate, ConnectionSecretCrypto connectionSecretCrypto) {
        this.jdbcTemplate = jdbcTemplate;
        this.connectionSecretCrypto = connectionSecretCrypto;
    }

    public UserDbConnectionCreateResponse create(UUID workspaceId, UserDbConnectionCreateRequest request) {
        if (request == null) {
            throw new UserWasException(400, "request body is required");
        }
        String name = required(request.name(), "name");
        String dbType = normalizeDbType(required(request.dbType(), "dbType"));
        String host = required(request.host(), "host");
        int port = request.port() == null ? 5432 : request.port();
        String dbName = required(request.dbName(), "dbName");
        String username = required(request.username(), "username");
        String sslMode = normalizeSslMode(request.sslMode());
        String secretRef = trimToNull(request.secretRef());
        String password = trimToNull(request.password());

        if (secretRef == null && password == null) {
            throw new UserWasException(400, "password or secretRef is required");
        }

        String passwordEnc = password == null ? null : connectionSecretCrypto.encrypt(password);
        Integer keyVersion = passwordEnc == null ? null : connectionSecretCrypto.keyVersion();

        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into db_connection " +
                        "(id, workspace_id, name, db_type, host, port, db_name, username, secret_ref, ssl_mode, password_enc, key_version, active, created_at, updated_at) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, now(), now())",
                id, workspaceId, name, dbType, host, port, dbName, username, secretRef, sslMode, passwordEnc, keyVersion
        );
        return new UserDbConnectionCreateResponse(id);
    }

    public UserDbConnectionListResponse list(UUID workspaceId) {
        List<UserDbConnectionItemResponse> items = jdbcTemplate.query(
                "select id, name, db_type, host, port, db_name, username, secret_ref, ssl_mode, key_version, active, created_at, updated_at " +
                        "from db_connection where workspace_id = ? order by created_at desc",
                (rs, n) -> new UserDbConnectionItemResponse(
                        (UUID) rs.getObject("id"),
                        rs.getString("name"),
                        rs.getString("db_type"),
                        rs.getString("host"),
                        rs.getInt("port"),
                        rs.getString("db_name"),
                        rs.getString("username"),
                        rs.getString("secret_ref"),
                        rs.getString("ssl_mode"),
                        rs.getObject("key_version") == null ? null : rs.getInt("key_version"),
                        rs.getBoolean("active"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                ),
                workspaceId
        );
        return new UserDbConnectionListResponse(items);
    }

    public UserDbConnectionTestResponse test(UUID workspaceId, UUID connectionId) {
        DbConnection connection = jdbcTemplate.query(
                "select id, workspace_id, db_type, host, port, db_name, username, secret_ref, ssl_mode, password_enc, active " +
                        "from db_connection where id = ? and workspace_id = ?",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    return new DbConnection(
                            (UUID) rs.getObject("id"),
                            rs.getString("db_type"),
                            rs.getString("host"),
                            rs.getInt("port"),
                            rs.getString("db_name"),
                            rs.getString("username"),
                            rs.getString("secret_ref"),
                            rs.getString("ssl_mode"),
                            rs.getString("password_enc"),
                            rs.getBoolean("active")
                    );
                },
                connectionId, workspaceId
        );

        if (connection == null) {
            throw new UserWasException(404, "db connection not found");
        }
        if (!connection.active()) {
            throw new UserWasException(400, "db connection is inactive");
        }
        if (!"POSTGRESQL".equals(connection.dbType())) {
            throw new UserWasException(400, "unsupported dbType: " + connection.dbType());
        }

        String password = null;
        if (connection.passwordEnc() != null) {
            password = connectionSecretCrypto.decrypt(connection.passwordEnc());
        } else if (connection.secretRef() != null) {
            throw new UserWasException(400, "secretRef connection test is not available without secret manager integration");
        }

        String jdbcUrl = "jdbc:postgresql://" + connection.host() + ":" + connection.port() + "/" + connection.dbName()
                + "?sslmode=" + connection.sslMode();
        try {
            DriverManager.setLoginTimeout(5);
            Properties props = new Properties();
            props.setProperty("user", connection.username());
            if (password != null) {
                props.setProperty("password", password);
            }
            try (var ignored = DriverManager.getConnection(jdbcUrl, props)) {
                return new UserDbConnectionTestResponse(true, "connection successful");
            }
        } catch (Exception e) {
            return new UserDbConnectionTestResponse(false, "connection failed: " + e.getMessage());
        }
    }

    private String normalizeDbType(String dbType) {
        String value = dbType.trim().toUpperCase();
        if (!"POSTGRESQL".equals(value)) {
            throw new UserWasException(400, "dbType must be POSTGRESQL");
        }
        return value;
    }

    private String normalizeSslMode(String sslMode) {
        String value = sslMode == null || sslMode.isBlank() ? "disable" : sslMode.trim().toLowerCase();
        return switch (value) {
            case "disable", "allow", "prefer", "require", "verify-ca", "verify-full" -> value;
            default -> throw new UserWasException(400, "invalid sslMode");
        };
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new UserWasException(400, field + " is required");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record DbConnection(
            UUID id,
            String dbType,
            String host,
            int port,
            String dbName,
            String username,
            String secretRef,
            String sslMode,
            String passwordEnc,
            boolean active
    ) {
    }
}
