package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.ReportResponse;
import com.smartexpense.entity.Budget;
import com.smartexpense.entity.BudgetCategory;
import com.smartexpense.entity.Expense;
import com.smartexpense.entity.Income;
import com.smartexpense.exception.InvalidReportRangeException;
import com.smartexpense.repository.BudgetRepository;
import com.smartexpense.repository.ExpenseRepository;
import com.smartexpense.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;
    private final CurrentUserProvider currentUserProvider;

    public ReportService(ExpenseRepository expenseRepository, IncomeRepository incomeRepository,
                         BudgetRepository budgetRepository, CurrentUserProvider currentUserProvider) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.budgetRepository = budgetRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public ReportResponse generate(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        var userId = currentUserProvider.getCurrentUser().getId();
        List<Expense> expenses = expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId).stream()
                .filter(item -> inRange(item.getExpenseDate(), startDate, endDate)).toList();
        List<Income> income = incomeRepository.findAllByUserIdOrderByIncomeDateDesc(userId).stream()
                .filter(item -> inRange(item.getIncomeDate(), startDate, endDate)).toList();
        List<Budget> budgets = budgetRepository.findAllByUserIdOrderByYearDescMonthDesc(userId).stream()
                .filter(item -> overlaps(item, startDate, endDate)).toList();

        List<ReportResponse.ExpenseRow> expenseRows = expenses.stream()
                .map(item -> new ReportResponse.ExpenseRow(item.getExpenseDate(), item.getCategory().name(), item.getAmount(), item.getDescription())).toList();
        List<ReportResponse.IncomeRow> incomeRows = income.stream()
                .map(item -> new ReportResponse.IncomeRow(item.getIncomeDate(), item.getSource().name(), item.getAmount(), item.getDescription())).toList();
        List<ReportResponse.BudgetRow> budgetRows = budgetRows(budgets, expenses);
        BigDecimal totalIncome = income.stream().map(Income::getAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenses.stream().map(Expense::getAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal totalBudget = budgetRows.stream().map(ReportResponse.BudgetRow::amount).reduce(ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgetRows.stream().map(ReportResponse.BudgetRow::spent).reduce(ZERO, BigDecimal::add);
        return new ReportResponse(expenseRows, incomeRows, budgetRows,
                new ReportResponse.Summary(totalIncome, totalExpenses, totalIncome.subtract(totalExpenses), totalBudget, totalBudget.subtract(totalSpent).max(ZERO)));
    }

    @Transactional(readOnly = true)
    public String export(String type, LocalDate startDate, LocalDate endDate) {
        ReportResponse report = generate(startDate, endDate);
        return switch (type.toLowerCase()) {
            case "expenses" -> expenseCsv(report.expenses());
            case "income" -> incomeCsv(report.income());
            case "financial" -> financialCsv(report);
            default -> throw new InvalidReportRangeException("Report type must be expenses, income, or financial");
        };
    }

    private List<ReportResponse.BudgetRow> budgetRows(List<Budget> budgets, List<Expense> expenses) {
        List<ReportResponse.BudgetRow> rows = new ArrayList<>();
        for (Budget budget : budgets) {
            if (budget.getCategories().isEmpty()) {
                BigDecimal spent = expenses.stream().filter(item -> item.getExpenseDate().getYear() == budget.getYear()
                                && item.getExpenseDate().getMonthValue() == budget.getMonth()).map(Expense::getAmount).reduce(ZERO, BigDecimal::add);
                BigDecimal amount = budget.getTotalLimit();
                BigDecimal percentage = amount.signum() == 0 ? ZERO : spent.multiply(BigDecimal.valueOf(100)).divide(amount, 2, RoundingMode.HALF_UP);
                rows.add(new ReportResponse.BudgetRow("OTHER", budget.getMonth(), budget.getYear(), amount, spent,
                        amount.subtract(spent).max(ZERO), percentage, status(percentage)));
                continue;
            }
            for (BudgetCategory category : budget.getCategories()) {
                BigDecimal spent = expenses.stream().filter(item -> item.getExpenseDate().getYear() == budget.getYear()
                                && item.getExpenseDate().getMonthValue() == budget.getMonth()
                                && item.getCategory() == category.getCategory()).map(Expense::getAmount).reduce(ZERO, BigDecimal::add);
                BigDecimal amount = category.getSpendingLimit();
                BigDecimal percentage = amount.signum() == 0 ? ZERO : spent.multiply(BigDecimal.valueOf(100)).divide(amount, 2, RoundingMode.HALF_UP);
                rows.add(new ReportResponse.BudgetRow(category.getCategory().name(), budget.getMonth(), budget.getYear(), amount,
                        spent, amount.subtract(spent).max(ZERO), percentage, status(percentage)));
            }
        }
        return rows;
    }

    private String expenseCsv(List<ReportResponse.ExpenseRow> rows) {
        StringBuilder csv = new StringBuilder("Date,Category,Amount,Description\n");
        rows.forEach(row -> csv.append(row.date()).append(',').append(csv(row.category())).append(',').append(row.amount()).append(',').append(csv(row.description())).append('\n'));
        return csv.toString();
    }

    private String incomeCsv(List<ReportResponse.IncomeRow> rows) {
        StringBuilder csv = new StringBuilder("Date,Source,Amount,Description\n");
        rows.forEach(row -> csv.append(row.date()).append(',').append(csv(row.source())).append(',').append(row.amount()).append(',').append(csv(row.description())).append('\n'));
        return csv.toString();
    }

    private String financialCsv(ReportResponse report) {
        StringBuilder csv = new StringBuilder("Type,Date,Category/Source,Amount,Description\n");
        report.expenses().forEach(row -> csv.append("Expense,").append(row.date()).append(',').append(csv(row.category())).append(',').append(row.amount()).append(',').append(csv(row.description())).append('\n'));
        report.income().forEach(row -> csv.append("Income,").append(row.date()).append(',').append(csv(row.source())).append(',').append(row.amount()).append(',').append(csv(row.description())).append('\n'));
        return csv.toString();
    }

    private String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"") ? "\"" + escaped + "\"" : escaped;
    }

    private boolean inRange(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private boolean overlaps(Budget budget, LocalDate start, LocalDate end) {
        LocalDate budgetStart = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate budgetEnd = budgetStart.withDayOfMonth(budgetStart.lengthOfMonth());
        return !budgetEnd.isBefore(start) && !budgetStart.isAfter(end);
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) throw new InvalidReportRangeException("Start date and end date are required");
        if (start.isAfter(end)) throw new InvalidReportRangeException("Start date cannot be after end date");
    }

    private String status(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.valueOf(100)) >= 0) return "EXCEEDED";
        if (percentage.compareTo(BigDecimal.valueOf(75)) >= 0) return "WARNING";
        return "ON_TRACK";
    }
}
