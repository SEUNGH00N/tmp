package com.example.excelimport.config;

import com.example.excelimport.auth.AuthInterceptor;
import com.example.excelimport.auth.AdminAuthorizationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminAuthorizationInterceptor adminAuthorizationInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, AdminAuthorizationInterceptor adminAuthorizationInterceptor) {
        this.authInterceptor = authInterceptor;
        this.adminAuthorizationInterceptor = adminAuthorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/mock/**")
                .excludePathPatterns("/mock/screens/**", "/mock/app.js");

        registry.addInterceptor(adminAuthorizationInterceptor)
                .addPathPatterns("/api/v1/admin/**", "/mock/admin-users.html", "/mock/admin-roles.html");
    }
}
