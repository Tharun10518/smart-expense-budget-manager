package com.smartexpense.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "income_records")
public class Income extends BaseEntity {

    @NotNull
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotNull
    @Column(name = "income_date", nullable = false)
    private LocalDate incomeDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private IncomeSource source;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_income_user"))
    private User user;

    protected Income() {
    }

    public Income(User user, BigDecimal amount, LocalDate incomeDate, IncomeSource source, String description) {
        this.user = user;
        this.amount = amount;
        this.incomeDate = incomeDate;
        this.source = source;
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getIncomeDate() {
        return incomeDate;
    }

    public IncomeSource getSource() {
        return source;
    }

    public String getDescription() {
        return description;
    }

    public User getUser() {
        return user;
    }

    public void update(BigDecimal amount, LocalDate incomeDate, IncomeSource source, String description) {
        this.amount = amount;
        this.incomeDate = incomeDate;
        this.source = source;
        this.description = description;
    }
}
