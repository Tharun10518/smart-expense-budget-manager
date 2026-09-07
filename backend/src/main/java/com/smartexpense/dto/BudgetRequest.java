package com.smartexpense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.smartexpense.entity.ExpenseCategory;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotNull(message = "Year is required")
        @Min(value = 2000, message = "Year must be 2000 or later")
        @Max(value = 2100, message = "Year must be 2100 or earlier")
        Integer year,
        @NotNull(message = "Month is required")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        Integer month,
        @NotNull(message = "Category is required")
        ExpenseCategory category,
        @NotNull(message = "Total limit is required")
        @DecimalMin(value = "0.01", message = "Total limit must be greater than 0")
        BigDecimal totalLimit) {
}
