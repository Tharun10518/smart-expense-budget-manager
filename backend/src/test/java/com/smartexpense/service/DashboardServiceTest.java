package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.DashboardResponse;
import com.smartexpense.entity.Budget;
import com.smartexpense.entity.Expense;
import com.smartexpense.entity.ExpenseCategory;
import com.smartexpense.entity.Income;
import com.smartexpense.entity.IncomeSource;
import com.smartexpense.entity.PaymentMethod;
import com.smartexpense.entity.User;
import com.smartexpense.repository.BudgetRepository;
import com.smartexpense.repository.ExpenseRepository;
import com.smartexpense.repository.IncomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private User user;

    private final UUID userId = UUID.randomUUID();
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(expenseRepository, incomeRepository, budgetRepository, currentUserProvider);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(budgetRepository.findAllByUserIdOrderByYearDescMonthDesc(userId)).thenReturn(List.of());
    }

    @Test
    void calculatesTotalsBalanceAndCurrentMonthValues() {
        LocalDate today = LocalDate.now();
        Expense currentExpense = new Expense(user, new BigDecimal("120.00"), today, ExpenseCategory.FOOD, "Lunch", PaymentMethod.CASH);
        Income currentIncome = new Income(user, new BigDecimal("500.00"), today, IncomeSource.SALARY, "Salary");
        Expense previousExpense = new Expense(user, new BigDecimal("50.00"), today.minusMonths(1), ExpenseCategory.FOOD, "Old lunch", PaymentMethod.CASH);
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of(currentExpense, previousExpense));
        when(incomeRepository.findAllByUserIdOrderByIncomeDateDesc(userId)).thenReturn(List.of(currentIncome));

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(new BigDecimal("500.00"), response.totalIncome());
        assertEquals(new BigDecimal("170.00"), response.totalExpenses());
        assertEquals(new BigDecimal("330.00"), response.totalBalance());
        assertEquals(new BigDecimal("500.00"), response.monthlyIncome());
        assertEquals(new BigDecimal("120.00"), response.monthlyExpenses());
        assertEquals(new BigDecimal("380.00"), response.monthlyBalance());
        assertEquals(BigDecimal.ZERO, response.previousMonthIncome());
        assertEquals(new BigDecimal("50.00"), response.previousMonthExpenses());
        assertEquals(new BigDecimal("76.00"), response.monthlySavingsPercentage());
        verify(expenseRepository).findAllByUserIdOrderByExpenseDateDesc(userId);
        verify(incomeRepository).findAllByUserIdOrderByIncomeDateDesc(userId);
    }

    @Test
    void calculatesBudgetRemainingPercentageAndCategoryTotals() {
        LocalDate today = LocalDate.now();
        Budget budget = new Budget(user, today.getYear(), today.getMonthValue(), new BigDecimal("500.00"));
        Expense food = new Expense(user, new BigDecimal("120.00"), today, ExpenseCategory.FOOD, "Food", PaymentMethod.CASH);
        Expense transport = new Expense(user, new BigDecimal("80.00"), today, ExpenseCategory.TRANSPORTATION, "Bus", PaymentMethod.CASH);
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of(food, transport));
        when(incomeRepository.findAllByUserIdOrderByIncomeDateDesc(userId)).thenReturn(List.of());
        when(budgetRepository.findAllByUserIdOrderByYearDescMonthDesc(userId)).thenReturn(List.of(budget));

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(new BigDecimal("500.00"), response.budget().total());
        assertEquals(new BigDecimal("200.00"), response.budget().spent());
        assertEquals(new BigDecimal("300.00"), response.budget().remaining());
        assertEquals(new BigDecimal("40.00"), response.budget().percentageUsed());
        assertEquals(2, response.expensesByCategory().size());
        assertEquals(new BigDecimal("120.00"), response.expensesByCategory().get(0).amount());
    }

    @Test
    void buildsLatestMonthsAndHandlesEmptyData() {
        LocalDate today = LocalDate.now();
        Income income = new Income(user, new BigDecimal("100.00"), today.minusMonths(1), IncomeSource.GIFT, "Gift");
        Expense expense = new Expense(user, new BigDecimal("40.00"), today, ExpenseCategory.OTHER, "Other", PaymentMethod.CASH);
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of(expense));
        when(incomeRepository.findAllByUserIdOrderByIncomeDateDesc(userId)).thenReturn(List.of(income));

        DashboardResponse response = dashboardService.getDashboard();

        assertTrue(response.monthlyTrend().stream().anyMatch(item -> item.month().equals(YearMonth.from(today).toString())));
        assertTrue(response.monthlyTrend().stream().anyMatch(item -> item.month().equals(YearMonth.from(today.minusMonths(1)).toString())));
        assertEquals(1, response.transactionCount());
    }

    @Test
    void emptyUserDataProducesZeroSafeDashboard() {
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of());
        when(incomeRepository.findAllByUserIdOrderByIncomeDateDesc(userId)).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(BigDecimal.ZERO, response.totalIncome());
        assertEquals(BigDecimal.ZERO, response.totalExpenses());
        assertEquals(BigDecimal.ZERO, response.totalBalance());
        assertEquals(BigDecimal.ZERO, response.budget().percentageUsed());
        assertTrue(response.monthlyTrend().isEmpty());
    }
}
