package com.tasksync.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasksync.backend.model.TeamMember;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findAllByUserId(Long userId);

    Optional<TeamMember> findByIdAndUserId(Long id, Long userId);
}
