package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.BudgetRequest;
import com.smartexpense.dto.BudgetResponse;
import com.smartexpense.dto.BudgetSummaryResponse;
import com.smartexpense.entity.Budget;
import com.smartexpense.entity.BudgetCategory;
import com.smartexpense.entity.Expense;
import com.smartexpense.entity.User;
import com.smartexpense.exception.BudgetConflictException;
import com.smartexpense.exception.ResourceNotFoundException;
import com.smartexpense.repository.BudgetCategoryRepository;
import com.smartexpense.repository.BudgetRepository;
import com.smartexpense.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

@Service
@Transactional
public class BudgetService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final ExpenseRepository expenseRepository;
    private final CurrentUserProvider currentUserProvider;

    public BudgetService(BudgetRepository budgetRepository, BudgetCategoryRepository budgetCategoryRepository,
                         ExpenseRepository expenseRepository, CurrentUserProvider currentUserProvider) {
        this.budgetRepository = budgetRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.expenseRepository = expenseRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public BudgetResponse create(BudgetRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Budget budget = budgetRepository.findByUserIdAndYearAndMonth(user.getId(), request.year(), request.month())
                .orElseGet(() -> new Budget(user, request.year(), request.month(), ZERO));
        ensureCategoryAvailable(budget, request.category(), null);
        BudgetCategory category = new BudgetCategory(budget, request.category(), request.totalLimit());
        budget.getCategories().add(category);
        budget.recalculateTotal();
        Budget saved = budgetRepository.save(budget);
        return toResponse(category, savedExpenses());
    }

        @Transactional
    public List<BudgetResponse> findAll() {
        List<Expense> expenses = savedExpenses();
        return budgetRepository.findAllByUserIdOrderByYearDescMonthDesc(currentUserProvider.getCurrentUser().getId())
                .stream()
            .map(this::normalizeLegacyBudget)
            .flatMap(budget -> budget.getCategories().stream().map(category -> toResponse(category, expenses)))
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse findById(UUID id) {
        return toResponse(findOwned(id), savedExpenses());
    }

    @Transactional(readOnly = true)
    public BudgetSummaryResponse summary() {
        LocalDate today = LocalDate.now();
        List<BudgetResponse> currentBudgets = findAll().stream()
                .filter(item -> item.year() == today.getYear() && item.month() == today.getMonthValue())
                .toList();
        BigDecimal totalBudget = currentBudgets.stream().map(BudgetResponse::totalLimit).reduce(ZERO, BigDecimal::add);
        BigDecimal totalSpent = currentBudgets.stream().map(BudgetResponse::spent).reduce(ZERO, BigDecimal::add);
        BigDecimal remaining = totalBudget.subtract(totalSpent).max(ZERO);
        BigDecimal percentage = totalBudget.signum() == 0 ? ZERO : totalSpent.multiply(BigDecimal.valueOf(100)).divide(totalBudget, 2, RoundingMode.HALF_UP);
        return new BudgetSummaryResponse(totalBudget, totalSpent, remaining, percentage,
                currentBudgets.size(), (int) currentBudgets.stream().filter(item -> "EXCEEDED".equals(item.status())).count(),
                (int) currentBudgets.stream().filter(item -> "WARNING".equals(item.status())).count());
    }

    public BudgetResponse update(UUID id, BudgetRequest request) {
        BudgetCategory category = findOwned(id);
        Budget oldBudget = category.getBudget();
        Budget targetBudget = budgetRepository.findByUserIdAndYearAndMonth(currentUserProvider.getCurrentUser().getId(), request.year(), request.month())
                .orElse(oldBudget);
        ensureCategoryAvailable(targetBudget, request.category(), targetBudget == oldBudget ? category : null);

        if (targetBudget != oldBudget) {
            oldBudget.getCategories().remove(category);
            oldBudget.recalculateTotal();
            category.assignBudget(targetBudget);
            targetBudget.getCategories().add(category);
            targetBudget.recalculateTotal();
            budgetRepository.save(oldBudget);
        } else {
            category.update(request.category(), request.totalLimit());
            targetBudget.recalculateTotal();
        }
        category.update(request.category(), request.totalLimit());
        Budget saved = budgetRepository.save(targetBudget);
        return toResponse(category, savedExpenses());
    }

    public void delete(UUID id) {
        BudgetCategory category = findOwned(id);
        Budget budget = category.getBudget();
        budget.getCategories().remove(category);
        budget.recalculateTotal();
        budgetCategoryRepository.delete(category);
        if (budget.getCategories().isEmpty()) {
            budgetRepository.delete(budget);
        } else {
            budgetRepository.save(budget);
        }
    }

    private BudgetCategory findOwned(UUID id) {
        return budgetCategoryRepository.findByIdAndBudgetUserId(id, currentUserProvider.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    private void ensureCategoryAvailable(Budget budget, com.smartexpense.entity.ExpenseCategory category, BudgetCategory ignored) {
        boolean duplicate = budget.getCategories().stream().anyMatch(item -> item != ignored && item.getCategory() == category);
        if (duplicate) {
            throw new BudgetConflictException("A budget for this category and month already exists");
        }
    }

    private List<Expense> savedExpenses() {
        return expenseRepository.findAllByUserIdOrderByExpenseDateDesc(currentUserProvider.getCurrentUser().getId());
    }

    private BudgetResponse toResponse(BudgetCategory category, List<Expense> expenses) {
        Budget budget = category.getBudget();
        BigDecimal spent = expenses.stream()
                .filter(expense -> expense.getExpenseDate().getYear() == budget.getYear()
                        && expense.getExpenseDate().getMonthValue() == budget.getMonth()
                        && expense.getCategory() == category.getCategory())
                .map(Expense::getAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal limit = category.getSpendingLimit();
        return new BudgetResponse(category.getId(), budget.getYear(), budget.getMonth(), category.getCategory(), budget.getPeriod(),
            limit, spent, limit.subtract(spent).max(ZERO), percentage(spent, limit), status(spent, limit), budget.getCreatedAt(), budget.getUpdatedAt());
    }

    private Budget normalizeLegacyBudget(Budget budget) {
        if (budget.getCategories().isEmpty()) {
            budget.getCategories().add(new BudgetCategory(budget, com.smartexpense.entity.ExpenseCategory.OTHER, budget.getTotalLimit()));
            budget.recalculateTotal();
            return budgetRepository.save(budget);
        }
        return budget;
    }

    private BigDecimal percentage(BigDecimal spent, BigDecimal limit) {
        return limit.signum() == 0 ? ZERO : spent.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP);
    }

    private String status(BigDecimal spent, BigDecimal limit) {
        BigDecimal percentage = percentage(spent, limit);
        if (percentage.compareTo(BigDecimal.valueOf(100)) >= 0) return "EXCEEDED";
        if (percentage.compareTo(BigDecimal.valueOf(75)) >= 0) return "WARNING";
        return "ON_TRACK";
    }
}
