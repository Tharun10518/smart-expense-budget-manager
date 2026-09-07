package com.smartexpense.dto;

import com.smartexpense.entity.ExpenseCategory;
import com.smartexpense.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        BigDecimal amount,
        ExpenseCategory category,
        LocalDate date,
        String description,
        PaymentMethod paymentMethod,
        Instant createdAt,
        Instant updatedAt) {
}
