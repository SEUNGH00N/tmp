package com.example.excelimport.auth;

import com.example.excelimport.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionKeys.AUTH_USER) != null) {
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
}
