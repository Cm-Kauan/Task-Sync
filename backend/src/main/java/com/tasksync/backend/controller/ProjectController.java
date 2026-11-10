package com.tasksync.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tasksync.backend.model.Project;
import com.tasksync.backend.model.User;
import com.tasksync.backend.repository.ProjectRepository;
import com.tasksync.backend.security.CurrentUserService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;

    public ProjectController(ProjectRepository projectRepository, CurrentUserService currentUserService) {
        this.projectRepository = projectRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<Project> findAll(Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        return projectRepository.findAllByUserId(user.getId());
    }

    @PostMapping
    public Project create(@RequestBody Project project, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        project.setId(null);
        project.setUserId(user.getId());
        return projectRepository.save(project);
    }

    @PutMapping("/{id}")
    public Project update(@PathVariable Long id, @RequestBody Project project, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        projectRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto não encontrado"));

        project.setId(id);
        project.setUserId(user.getId());
        return projectRepository.save(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        Project existing = projectRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto não encontrado"));

        projectRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }
}
