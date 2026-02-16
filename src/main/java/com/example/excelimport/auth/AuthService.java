package com.example.excelimport.auth;

import com.example.excelimport.auth.entity.AppUser;
import com.example.excelimport.auth.repository.AppUserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;

    public AuthService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public Optional<AppUser> authenticate(String username, String password) {
        return appUserRepository.findByUsernameAndActiveTrue(username)
                .filter(user -> user.getPassword().equals(password));
    }
}
