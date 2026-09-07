package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.ExpenseRequest;
import com.smartexpense.dto.ExpenseResponse;
import com.smartexpense.entity.Expense;
import com.smartexpense.entity.ExpenseCategory;
import com.smartexpense.entity.PaymentMethod;
import com.smartexpense.entity.User;
import com.smartexpense.exception.ResourceNotFoundException;
import com.smartexpense.repository.ExpenseRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private User user;

    private ExpenseService expenseService;
    private final UUID userId = UUID.randomUUID();
    private final UUID expenseId = UUID.randomUUID();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(expenseRepository, currentUserProvider);
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(user);
    }

    @Test
    void createExpenseSavesAndReturnsResponse() {
        ExpenseRequest request = request();
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponse response = expenseService.create(request);

        assertEquals(request.amount(), response.amount());
        assertEquals(request.category(), response.category());
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    void getExpenseReturnsOwnedExpense() {
        when(user.getId()).thenReturn(userId);
        Expense expense = expense();
        when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(Optional.of(expense));

        ExpenseResponse response = expenseService.findById(expenseId);

        assertEquals(expense.getAmount(), response.amount());
        verify(expenseRepository).findByIdAndUserId(expenseId, userId);
    }

    @Test
    void listReturnsOnlyRepositoryOwnedExpenses() {
        when(user.getId()).thenReturn(userId);
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of(expense()));

        assertEquals(1, expenseService.findAll().size());
        verify(expenseRepository).findAllByUserIdOrderByExpenseDateDesc(userId);
    }

    @Test
    void updateExpenseMutatesAndSavesExpense() {
        when(user.getId()).thenReturn(userId);
        Expense expense = expense();
        ExpenseRequest request = request();
        when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(expense)).thenReturn(expense);

        ExpenseResponse response = expenseService.update(expenseId, request);

        assertEquals(request.paymentMethod(), response.paymentMethod());
        verify(expenseRepository).save(expense);
    }

    @Test
    void deleteExpenseDeletesOwnedExpense() {
        when(user.getId()).thenReturn(userId);
        Expense expense = expense();
        when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(Optional.of(expense));

        expenseService.delete(expenseId);

        verify(expenseRepository).delete(expense);
    }

    @Test
    void expenseOwnedByAnotherUserIsNotAccessible() {
        when(user.getId()).thenReturn(userId);
        when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> expenseService.findById(expenseId));
    }

    @Test
    void invalidAmountFailsRequestValidation() {
        ExpenseRequest invalid = new ExpenseRequest(BigDecimal.ZERO, ExpenseCategory.FOOD, LocalDate.now(), "Invalid", PaymentMethod.CASH);

        assertEquals(1, validator.validate(invalid).size());
    }

    private ExpenseRequest request() {
        return new ExpenseRequest(new BigDecimal("42.50"), ExpenseCategory.FOOD, LocalDate.of(2026, 9, 3), "Lunch", PaymentMethod.DEBIT_CARD);
    }

    private Expense expense() {
        return new Expense(user, new BigDecimal("42.50"), LocalDate.of(2026, 9, 3), ExpenseCategory.FOOD, "Lunch", PaymentMethod.DEBIT_CARD);
    }
}
