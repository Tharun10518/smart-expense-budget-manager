package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.ReportResponse;
import com.smartexpense.entity.Expense;
import com.smartexpense.entity.ExpenseCategory;
import com.smartexpense.entity.Income;
import com.smartexpense.entity.IncomeSource;
import com.smartexpense.entity.PaymentMethod;
import com.smartexpense.entity.User;
import com.smartexpense.exception.InvalidReportRangeException;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private User user;

    private final UUID userId = UUID.randomUUID();
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(expenseRepository, incomeRepository, budgetRepository, currentUserProvider);
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(user);
        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of());
        lenient().when(incomeRepository.findAllByUserIdOrderByIncomeDateDesc(userId)).thenReturn(List.of());
        lenient().when(budgetRepository.findAllByUserIdOrderByYearDescMonthDesc(userId)).thenReturn(List.of());
    }

    @Test
    void filtersOwnedRecordsAndCalculatesSummary() {
        Expense expense = new Expense(user, new BigDecimal("120.00"), LocalDate.of(2026, 9, 3), ExpenseCategory.FOOD, "Lunch", PaymentMethod.CASH);
        Income income = new Income(user, new BigDecimal("500.00"), LocalDate.of(2026, 9, 1), IncomeSource.SALARY, "Salary");
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of(expense));
        when(incomeRepository.findAllByUserIdOrderByIncomeDateDesc(userId)).thenReturn(List.of(income));

        ReportResponse report = reportService.generate(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertEquals(1, report.expenses().size());
        assertEquals(1, report.income().size());
        assertEquals(new BigDecimal("500.00"), report.summary().totalIncome());
        assertEquals(new BigDecimal("120.00"), report.summary().totalExpenses());
        assertEquals(new BigDecimal("380.00"), report.summary().totalSavings());
        verify(expenseRepository).findAllByUserIdOrderByExpenseDateDesc(userId);
        verify(incomeRepository).findAllByUserIdOrderByIncomeDateDesc(userId);
    }

    @Test
    void emptyReportReturnsZeroTotals() {
        ReportResponse report = reportService.generate(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertEquals(0, report.expenses().size());
        assertEquals(0, report.income().size());
        assertEquals(BigDecimal.ZERO, report.summary().totalIncome());
        assertEquals(BigDecimal.ZERO, report.summary().totalExpenses());
        assertEquals(BigDecimal.ZERO, report.summary().totalRemaining());
    }

    @Test
    void invalidDateRangeIsRejected() {
        assertThrows(InvalidReportRangeException.class,
                () -> reportService.generate(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1)));
    }

    @Test
    void exportsExpenseCsv() {
        Expense expense = new Expense(user, new BigDecimal("120.00"), LocalDate.of(2026, 9, 3), ExpenseCategory.FOOD, "Lunch, work", PaymentMethod.CASH);
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(List.of(expense));

        String csv = reportService.export("expenses", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertEquals(true, csv.startsWith("Date,Category,Amount,Description"));
        assertEquals(true, csv.contains("\"Lunch, work\""));
    }
}
