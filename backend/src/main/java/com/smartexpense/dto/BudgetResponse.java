package com.smartexpense.dto;

import com.smartexpense.entity.BudgetPeriod;
import com.smartexpense.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        Integer year,
        Integer month,
        ExpenseCategory category,
        BudgetPeriod period,
        BigDecimal totalLimit,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentageUsed,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
