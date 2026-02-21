package com.example.excelimport.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.common.web.RequestIdFilter;
import com.example.excelimport.auth.AuthService;
import com.example.excelimport.auth.SessionKeys;
import com.example.excelimport.auth.TokenService;
import com.example.excelimport.auth.entity.AppUser;
import com.example.excelimport.dto.AuthStatusResponse;
import com.example.excelimport.dto.LoginRequest;
import com.example.excelimport.dto.LoginResponse;
import com.example.excelimport.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        if (request == null || request.username() == null || request.password() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "https://example.com/problems/bad-request",
                    "Bad Request",
                    "username and password are required");
        }

        AppUser user = authService.authenticate(request.username(), request.password())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "https://example.com/problems/unauthorized",
                        "Unauthorized",
                        "invalid username or password"));

        HttpSession session = servletRequest.getSession(true);
        String accessToken = tokenService.issue(user.getUsername());
        session.setAttribute(SessionKeys.AUTH_USER, user.getUsername());
        session.setAttribute(SessionKeys.AUTH_TOKEN, accessToken);
        return ApiResponse.success(new LoginResponse(user.getUsername(), "Bearer", accessToken), requestId(servletRequest));
    }

    @GetMapping("/me")
    public ApiResponse<AuthStatusResponse> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String username = session == null ? null : (String) session.getAttribute(SessionKeys.AUTH_USER);
        if (username == null || username.isBlank()) {
            username = tokenService.resolveUsername(extractBearerToken(request));
        }
        boolean authenticated = username != null;
        return ApiResponse.success(new AuthStatusResponse(authenticated, username), requestId(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String token = extractBearerToken(request);
        if (token == null && session != null) {
            token = (String) session.getAttribute(SessionKeys.AUTH_TOKEN);
        }
        tokenService.revoke(token);
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.success(null, requestId(request));
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

    private String requestId(HttpServletRequest request) {
        Object attr = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return attr == null ? "" : attr.toString();
    }
}
