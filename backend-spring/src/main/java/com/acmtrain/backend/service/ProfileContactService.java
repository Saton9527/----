package com.acmtrain.backend.service;

import com.acmtrain.backend.dto.ContactEmailResponse;
import com.acmtrain.backend.dto.UpdateContactEmailRequest;
import com.acmtrain.backend.entity.UserAccountEntity;
import com.acmtrain.backend.repository.UserAccountRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileContactService {

    private final UserAccountRepository userAccountRepository;

    public ProfileContactService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public ContactEmailResponse getMyContactEmail(Long userId) {
        UserAccountEntity user = findUser(userId);
        return new ContactEmailResponse(user.getEmail());
    }

    @Transactional
    @CacheEvict(value = {"alerts"}, allEntries = true)
    public ContactEmailResponse updateMyContactEmail(Long userId, UpdateContactEmailRequest request) {
        UserAccountEntity user = findUser(userId);
        String normalized = request.email() == null ? null : request.email().trim();
        if (normalized != null && normalized.isEmpty()) {
            normalized = null;
        }

        if (normalized != null) {
            userAccountRepository.findByEmail(normalized)
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该邮箱已被其他账号使用");
                    });
        }

        user.setEmail(normalized);
        UserAccountEntity saved = userAccountRepository.save(user);
        return new ContactEmailResponse(saved.getEmail());
    }

    private UserAccountEntity findUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }
}
