package com.acmtrain.backend.controller;

import com.acmtrain.backend.dto.LoginRequest;
import com.acmtrain.backend.dto.LoginResponse;
import com.acmtrain.backend.dto.UserProfile;
import com.acmtrain.backend.entity.UserAccountEntity;
import com.acmtrain.backend.repository.UserAccountRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAccountRepository userAccountRepository;

    public AuthController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        UserAccountEntity user = userAccountRepository
                .findByUsernameAndPassword(request.username(), request.password())
                .orElse(null);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        return new LoginResponse(
                "demo-token-" + user.getId(),
                new UserProfile(user.getId(), user.getUsername(), user.getRealName(), user.getRole())
        );
    }
}
