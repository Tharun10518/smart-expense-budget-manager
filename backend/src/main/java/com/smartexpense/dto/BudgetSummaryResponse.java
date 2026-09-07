package com.smartexpense.dto;

import java.math.BigDecimal;

public record BudgetSummaryResponse(
        BigDecimal totalBudget,
        BigDecimal totalSpent,
        BigDecimal remainingAmount,
        BigDecimal percentageUsed,
        int budgetCount,
        int exceededBudgets,
        int warningBudgets) {
}