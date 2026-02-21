package com.example.excelimport.auth;

import com.example.excelimport.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        String username = session == null ? null : (String) session.getAttribute(SessionKeys.AUTH_USER);
        if (username == null || username.isBlank()) {
            username = tokenService.resolveUsername(extractBearerToken(request));
        }

        if (username != null && !username.isBlank()) {
            return true;
        }

        String uri = request.getRequestURI();
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

    private String extractBearerToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null) {
            return null;
        }
        String prefix = "Bearer ";
        if (!auth.startsWith(prefix)) {
            return null;
        }
        return auth.substring(prefix.length()).trim();
    }
}
