package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findByUsernameAndPassword(String username, String password);
    Optional<UserAccountEntity> findByUsername(String username);
    Optional<UserAccountEntity> findByEmail(String email);
    List<UserAccountEntity> findAllByRoleIgnoreCase(String role);
}
