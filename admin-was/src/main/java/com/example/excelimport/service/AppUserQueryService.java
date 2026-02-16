package com.example.excelimport.service;

import com.example.excelimport.dto.AppUserItem;
import com.example.excelimport.dto.PagedAppUsersResponse;
import com.example.excelimport.auth.repository.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserQueryService {

    private final AppUserRepository appUserRepository;

    public AppUserQueryService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public PagedAppUsersResponse getUsers(Pageable pageable) {
        Page<AppUserItem> page = appUserRepository.findAll(pageable)
                .map(u -> new AppUserItem(u.getId(), u.getUsername(), u.getDisplayName(), u.isActive(), u.getCreatedAt()));
        return new PagedAppUsersResponse(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
