package com.smartexpense.dto;

import com.smartexpense.entity.IncomeSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeResponse(
        UUID id,
        BigDecimal amount,
        IncomeSource source,
        LocalDate date,
        String description,
        Instant createdAt,
        Instant updatedAt) {
}
