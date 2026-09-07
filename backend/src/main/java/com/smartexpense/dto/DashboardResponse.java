package com.smartexpense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal totalBalance,
        BigDecimal remainingBudget,
        BigDecimal monthlyIncome,
        BigDecimal monthlyExpenses,
        BigDecimal monthlyBalance,
        BigDecimal previousMonthIncome,
        BigDecimal previousMonthExpenses,
        BigDecimal monthlySavingsPercentage,
        int transactionCount,
        List<Transaction> recentTransactions,
        List<CategoryTotal> expensesByCategory,
        List<MonthlyTotal> monthlyTrend,
        BudgetSummary budget) {

    public record Transaction(
            String type,
            String label,
            String description,
            LocalDate date,
            BigDecimal amount) {
    }

    public record CategoryTotal(String category, BigDecimal amount) {
    }

        public record MonthlyTotal(String month, BigDecimal income, BigDecimal expenses) {
        }

    public record BudgetSummary(
            BigDecimal total,
            BigDecimal spent,
            BigDecimal remaining,
            BigDecimal percentageUsed,
            String status) {
    }
}