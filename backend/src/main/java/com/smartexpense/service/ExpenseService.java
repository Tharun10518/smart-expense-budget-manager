package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.ExpenseRequest;
import com.smartexpense.dto.ExpenseResponse;
import com.smartexpense.entity.Expense;
import com.smartexpense.entity.User;
import com.smartexpense.exception.ResourceNotFoundException;
import com.smartexpense.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CurrentUserProvider currentUserProvider;

    public ExpenseService(ExpenseRepository expenseRepository, CurrentUserProvider currentUserProvider) {
        this.expenseRepository = expenseRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public ExpenseResponse create(ExpenseRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Expense expense = new Expense(user, request.amount(), request.date(), request.category(), request.description(), request.paymentMethod());
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findAll() {
        return expenseRepository.findAllByUserIdOrderByExpenseDateDesc(currentUserProvider.getCurrentUser().getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(UUID id) {
        return toResponse(findOwned(id));
    }

    public ExpenseResponse update(UUID id, ExpenseRequest request) {
        Expense expense = findOwned(id);
        expense.update(request.amount(), request.date(), request.category(), request.description(), request.paymentMethod());
        return toResponse(expenseRepository.save(expense));
    }

    public void delete(UUID id) {
        expenseRepository.delete(findOwned(id));
    }

    private Expense findOwned(UUID id) {
        return expenseRepository.findByIdAndUserId(id, currentUserProvider.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(expense.getId(), expense.getAmount(), expense.getCategory(), expense.getExpenseDate(),
                expense.getDescription(), expense.getPaymentMethod(), expense.getCreatedAt(), expense.getUpdatedAt());
    }
}
