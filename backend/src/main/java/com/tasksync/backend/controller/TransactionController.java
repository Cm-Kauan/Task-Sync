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

import com.tasksync.backend.model.Transaction;
import com.tasksync.backend.model.User;
import com.tasksync.backend.repository.TransactionRepository;
import com.tasksync.backend.security.CurrentUserService;

@RestController
@RequestMapping("/api/finance/lancamentos")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public TransactionController(TransactionRepository transactionRepository, CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<Transaction> findAll(Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        return transactionRepository.findAllByUserId(user.getId());
    }

    @PostMapping
    public Transaction create(@RequestBody Transaction transaction, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        transaction.setId(null);
        transaction.setUserId(user.getId());
        return transactionRepository.save(transaction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User user = currentUserService.resolve(authentication);
        Transaction existing = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento não encontrado"));

        transactionRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }
}
