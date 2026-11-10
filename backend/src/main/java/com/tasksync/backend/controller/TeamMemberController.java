package com.tasksync.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tasksync.backend.model.TeamMember;
import com.tasksync.backend.model.User;
import com.tasksync.backend.repository.TeamMemberRepository;
import com.tasksync.backend.security.CurrentUserService;

@RestController
@RequestMapping("/api/team")
public class TeamMemberController {

    private final TeamMemberRepository teamMemberRepository;
    private final CurrentUserService currentUserService;

    public TeamMemberController(TeamMemberRepository teamMemberRepository, CurrentUserService currentUserService) {
        this.teamMemberRepository = teamMemberRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<TeamMember> findAll(Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        return teamMemberRepository.findAllByUserId(user.getId());
    }

    @PostMapping
    public TeamMember create(@RequestBody TeamMember teamMember, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        teamMember.setId(null);
        teamMember.setUserId(user.getId());
        return teamMemberRepository.save(teamMember);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        TeamMember existing = teamMemberRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));

        teamMemberRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }
}
