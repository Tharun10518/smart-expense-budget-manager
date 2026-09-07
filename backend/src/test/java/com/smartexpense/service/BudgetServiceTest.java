package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.BudgetRequest;
import com.smartexpense.dto.BudgetResponse;
import com.smartexpense.dto.BudgetSummaryResponse;
import com.smartexpense.entity.Budget;
import com.smartexpense.entity.BudgetCategory;
import com.smartexpense.entity.Expense;
import com.smartexpense.entity.ExpenseCategory;
import com.smartexpense.entity.PaymentMethod;
import com.smartexpense.entity.User;
import com.smartexpense.exception.BudgetConflictException;
import com.smartexpense.exception.ResourceNotFoundException;
import com.smartexpense.repository.BudgetCategoryRepository;
import com.smartexpense.repository.BudgetRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private BudgetCategoryRepository budgetCategoryRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private User user;

    private final UUID userId = UUID.randomUUID();
    private BudgetService budgetService;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(user);
        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of());
        budgetService = new BudgetService(budgetRepository, budgetCategoryRepository, expenseRepository, currentUserProvider);
    }

    @Test
    void createBudgetSavesCategoryAndReturnsProgress() {
        when(budgetRepository.findByUserIdAndYearAndMonth(userId, 2026, 9)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BudgetRequest request = new BudgetRequest(2026, 9, ExpenseCategory.FOOD, new BigDecimal("3000.00"));

        BudgetResponse response = budgetService.create(request);

        assertEquals(request.year(), response.year());
        assertEquals(request.month(), response.month());
        assertEquals(request.category(), response.category());
        assertEquals(request.totalLimit(), response.totalLimit());
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void duplicateCategoryBudgetIsRejected() {
        Budget budget = new Budget(user, 2026, 9, new BigDecimal("3000.00"));
        budget.getCategories().add(new BudgetCategory(budget, ExpenseCategory.FOOD, new BigDecimal("3000.00")));
        when(budgetRepository.findByUserIdAndYearAndMonth(userId, 2026, 9)).thenReturn(Optional.of(budget));

        assertThrows(BudgetConflictException.class, () -> budgetService.create(new BudgetRequest(2026, 9, ExpenseCategory.FOOD, new BigDecimal("2000.00"))));
    }

    @Test
    void progressUsesOnlyOwnedCategoryExpenses() {
        Budget budget = new Budget(user, 2026, 9, new BigDecimal("3000.00"));
        BudgetCategory category = new BudgetCategory(budget, ExpenseCategory.FOOD, new BigDecimal("3000.00"));
        budget.getCategories().add(category);
        when(budgetCategoryRepository.findByIdAndBudgetUserId(any(UUID.class), eq(userId))).thenReturn(Optional.of(category));
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of(
                new Expense(user, new BigDecimal("650.00"), LocalDate.of(2026, 9, 3), ExpenseCategory.FOOD, "Lunch", PaymentMethod.DIGITAL_WALLET)));

        BudgetResponse response = budgetService.findById(UUID.randomUUID());

        assertEquals(new BigDecimal("650.00"), response.spent());
        assertEquals(new BigDecimal("2350.00"), response.remaining());
        assertEquals(new BigDecimal("21.67"), response.percentageUsed());
        assertEquals("ON_TRACK", response.status());
    }

    @Test
    void listReturnsOwnedCategoryBudgets() {
        Budget budget = new Budget(user, 2026, 9, new BigDecimal("3000.00"));
        budget.getCategories().add(new BudgetCategory(budget, ExpenseCategory.FOOD, new BigDecimal("3000.00")));
        when(budgetRepository.findAllByUserIdOrderByYearDescMonthDesc(userId)).thenReturn(List.of(budget));

        assertEquals(1, budgetService.findAll().size());
    }

    @Test
    void updateChangesCategoryLimit() {
        Budget budget = new Budget(user, 2026, 9, new BigDecimal("3000.00"));
        BudgetCategory category = new BudgetCategory(budget, ExpenseCategory.FOOD, new BigDecimal("3000.00"));
        budget.getCategories().add(category);
        when(budgetCategoryRepository.findByIdAndBudgetUserId(any(UUID.class), eq(userId))).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndYearAndMonth(userId, 2026, 9)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response = budgetService.update(UUID.randomUUID(), new BudgetRequest(2026, 9, ExpenseCategory.FOOD, new BigDecimal("3500.00")));

        assertEquals(new BigDecimal("3500.00"), response.totalLimit());
    }

    @Test
    void deleteRemovesOwnedCategoryBudget() {
        Budget budget = new Budget(user, 2026, 9, new BigDecimal("3000.00"));
        BudgetCategory category = new BudgetCategory(budget, ExpenseCategory.FOOD, new BigDecimal("3000.00"));
        budget.getCategories().add(category);
        when(budgetCategoryRepository.findByIdAndBudgetUserId(any(UUID.class), eq(userId))).thenReturn(Optional.of(category));

        budgetService.delete(UUID.randomUUID());

        verify(budgetCategoryRepository).delete(category);
        verify(budgetRepository).delete(budget);
    }

    @Test
    void invalidAmountFailsRequestValidation() {
        BudgetRequest request = new BudgetRequest(2026, 9, ExpenseCategory.FOOD, BigDecimal.ZERO);

        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void invalidCategoryFailsRequestValidation() {
        BudgetRequest request = new BudgetRequest(2026, 9, null, new BigDecimal("100.00"));

        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void warningAndExceededStatusesFollowPercentageThresholds() {
        Budget warningBudget = new Budget(user, 2026, 9, new BigDecimal("100.00"));
        BudgetCategory warningCategory = new BudgetCategory(warningBudget, ExpenseCategory.FOOD, new BigDecimal("100.00"));
        warningBudget.getCategories().add(warningCategory);
        when(budgetCategoryRepository.findByIdAndBudgetUserId(any(UUID.class), eq(userId))).thenReturn(Optional.of(warningCategory));
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of(
                new Expense(user, new BigDecimal("80.00"), LocalDate.of(2026, 9, 3), ExpenseCategory.FOOD, "Food", PaymentMethod.CASH)));
        assertEquals("WARNING", budgetService.findById(UUID.randomUUID()).status());

        Budget exceededBudget = new Budget(user, 2026, 9, new BigDecimal("100.00"));
        BudgetCategory exceededCategory = new BudgetCategory(exceededBudget, ExpenseCategory.FOOD, new BigDecimal("100.00"));
        exceededBudget.getCategories().add(exceededCategory);
        when(budgetCategoryRepository.findByIdAndBudgetUserId(any(UUID.class), eq(userId))).thenReturn(Optional.of(exceededCategory));
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of(
                new Expense(user, new BigDecimal("120.00"), LocalDate.of(2026, 9, 3), ExpenseCategory.FOOD, "Food", PaymentMethod.CASH)));
        assertEquals("EXCEEDED", budgetService.findById(UUID.randomUUID()).status());
    }

    @Test
    void summaryAggregatesCurrentMonthBudgets() {
        java.time.LocalDate today = java.time.LocalDate.now();
        Budget budget = new Budget(user, today.getYear(), today.getMonthValue(), new BigDecimal("100.00"));
        budget.getCategories().add(new BudgetCategory(budget, ExpenseCategory.FOOD, new BigDecimal("100.00")));
        when(budgetRepository.findAllByUserIdOrderByYearDescMonthDesc(userId)).thenReturn(List.of(budget));
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of(
                new Expense(user, new BigDecimal("80.00"), today, ExpenseCategory.FOOD, "Food", PaymentMethod.CASH)));

        BudgetSummaryResponse summary = budgetService.summary();

        assertEquals(new BigDecimal("100.00"), summary.totalBudget());
        assertEquals(new BigDecimal("80.00"), summary.totalSpent());
        assertEquals(new BigDecimal("20.00"), summary.remainingAmount());
        assertEquals(new BigDecimal("80.00"), summary.percentageUsed());
        assertEquals(1, summary.warningBudgets());
    }

    @Test
    void anotherUsersBudgetCannotBeLoaded() {
        when(budgetCategoryRepository.findByIdAndBudgetUserId(any(UUID.class), eq(userId))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> budgetService.findById(UUID.randomUUID()));
    }

    @Test
    void anotherUsersBudgetCannotBeUpdatedOrDeleted() {
        when(budgetCategoryRepository.findByIdAndBudgetUserId(any(UUID.class), eq(userId))).thenReturn(Optional.empty());
        BudgetRequest request = new BudgetRequest(2026, 9, ExpenseCategory.FOOD, new BigDecimal("100.00"));

        assertThrows(ResourceNotFoundException.class, () -> budgetService.update(UUID.randomUUID(), request));
        assertThrows(ResourceNotFoundException.class, () -> budgetService.delete(UUID.randomUUID()));
    }
}
