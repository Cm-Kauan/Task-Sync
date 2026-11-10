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

import com.tasksync.backend.model.Task;
import com.tasksync.backend.model.User;
import com.tasksync.backend.repository.TaskRepository;
import com.tasksync.backend.security.CurrentUserService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;

    public TaskController(TaskRepository taskRepository, CurrentUserService currentUserService) {
        this.taskRepository = taskRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<Task> findAll(Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        return taskRepository.findAllByUserId(user.getId());
    }

    @PostMapping
    public Task create(@RequestBody Task task, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        task.setId(null);
        task.setUserId(user.getId());
        return taskRepository.save(task);
    }

    @PutMapping("/{id}")
    public Task update(@PathVariable Long id, @RequestBody Task task, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));

        task.setId(id);
        task.setUserId(user.getId());
        return taskRepository.save(task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        Task existing = taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));

        taskRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }
}
