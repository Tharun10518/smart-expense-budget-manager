package com.smartexpense.dto;

import com.smartexpense.entity.IncomeSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,
        @NotNull(message = "Source is required")
        IncomeSource source,
        @NotNull(message = "Date is required")
        LocalDate date,
        @Size(max = 500, message = "Description must be 500 characters or fewer")
        String description) {
}
