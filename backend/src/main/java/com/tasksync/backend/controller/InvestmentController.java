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

import com.tasksync.backend.model.Investment;
import com.tasksync.backend.model.User;
import com.tasksync.backend.repository.InvestmentRepository;
import com.tasksync.backend.security.CurrentUserService;

@RestController
@RequestMapping("/api/finance/investments")
public class InvestmentController {

    private final InvestmentRepository investmentRepository;
    private final CurrentUserService currentUserService;

    public InvestmentController(InvestmentRepository investmentRepository, CurrentUserService currentUserService) {
        this.investmentRepository = investmentRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<Investment> findAll(Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        return investmentRepository.findAllByUserId(user.getId());
    }

    @PostMapping
    public Investment create(@RequestBody Investment investment, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        investment.setId(null);
        investment.setUserId(user.getId());
        return investmentRepository.save(investment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        Investment existing = investmentRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Investimento não encontrado"));

        investmentRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }
}
