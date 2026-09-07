package com.smartexpense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportResponse(
        List<ExpenseRow> expenses,
        List<IncomeRow> income,
        List<BudgetRow> budgets,
        Summary summary) {

    public record ExpenseRow(LocalDate date, String category, BigDecimal amount, String description) {
    }

    public record IncomeRow(LocalDate date, String source, BigDecimal amount, String description) {
    }

    public record BudgetRow(String category, Integer month, Integer year, BigDecimal amount,
                            BigDecimal spent, BigDecimal remaining, BigDecimal percentageUsed, String status) {
    }

    public record Summary(BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal totalSavings,
                          BigDecimal totalBudget, BigDecimal totalRemaining) {
    }
}
