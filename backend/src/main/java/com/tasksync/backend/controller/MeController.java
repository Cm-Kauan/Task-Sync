package com.tasksync.backend.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.tasksync.backend.model.User;
import com.tasksync.backend.repository.UserRepository;
import com.tasksync.backend.security.CurrentUserService;

import jakarta.validation.Valid;

import com.tasksync.backend.dto.UpdateProfileRequest;

/**
 * Endpoint protegido: só responde se a requisição vier com um JWT válido no
 * header Authorization. GET prova que a autenticação funciona de ponta a
 * ponta; PUT permite ao usuário editar nome/telefone do próprio perfil.
 */
@RestController
public class MeController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public MeController(CurrentUserService currentUserService, UserRepository userRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @GetMapping("/api/me")
    public Map<String, String> me(Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        return Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "phone", user.getPhone() != null ? user.getPhone() : ""
        );
    }

    @PutMapping("/api/me")
    public Map<String, String> updateProfile(@Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        user.setName(request.name());
        user.setPhone(request.phone());
        userRepository.save(user);
        return Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "phone", user.getPhone() != null ? user.getPhone() : ""
        );
    }
}
