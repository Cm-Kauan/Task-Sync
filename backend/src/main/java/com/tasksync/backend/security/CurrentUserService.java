package com.tasksync.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.tasksync.backend.model.User;
import com.tasksync.backend.repository.UserRepository;

/**
 * Resolve a entidade User correspondente ao token JWT autenticado na
 * requisição atual. Usado pelos controllers de dados (Task, Transaction,
 * Investment, Project, TeamMember) para filtrar/associar cada registro ao
 * dono correto.
 */
@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User resolve(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário autenticado não encontrado no banco: " + authentication.getName()));
    }
}
