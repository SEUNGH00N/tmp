package com.example.excelimport.auth;

import com.example.excelimport.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class AdminAuthorizationInterceptor implements HandlerInterceptor {

    private static final Set<String> READ_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "OPERATOR", "VIEWER");
    private static final Set<String> WRITE_ROLES = Set.of("SUPER_ADMIN", "ADMIN");

    private final RoleAuthorizationService roleAuthorizationService;

    public AdminAuthorizationInterceptor(RoleAuthorizationService roleAuthorizationService) {
        this.roleAuthorizationService = roleAuthorizationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        String username = session == null ? null : (String) session.getAttribute(SessionKeys.AUTH_USER);
        String uri = request.getRequestURI();

        if (username == null || username.isBlank()) {
            if (uri.startsWith("/mock/")) {
                response.setStatus(HttpStatus.FOUND.value());
                response.setHeader("Location", "/login.html");
                return false;
            }
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "https://example.com/problems/unauthorized",
                    "Unauthorized",
                    "login required");
        }

        Set<String> required = request.getMethod().equals("GET") ? READ_ROLES : WRITE_ROLES;
        if (!roleAuthorizationService.hasAnyRole(username, required)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "https://example.com/problems/forbidden",
                    "Forbidden",
                    "insufficient role");
        }
        return true;
    }
}
