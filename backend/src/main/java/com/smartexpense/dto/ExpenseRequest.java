package com.smartexpense.dto;

import com.smartexpense.entity.ExpenseCategory;
import com.smartexpense.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,
        @NotNull(message = "Category is required")
        ExpenseCategory category,
        @NotNull(message = "Date is required")
        LocalDate date,
        @Size(max = 500, message = "Description must be 500 characters or fewer")
        String description,
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod) {
}
