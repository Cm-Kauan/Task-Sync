package com.tasksync.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasksync.backend.model.Investment;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findAllByUserId(Long userId);

    Optional<Investment> findByIdAndUserId(Long id, Long userId);
}
