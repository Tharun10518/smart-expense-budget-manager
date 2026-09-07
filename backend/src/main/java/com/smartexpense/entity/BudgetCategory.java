package com.smartexpense.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "budget_categories", uniqueConstraints = @UniqueConstraint(name = "uk_budget_category", columnNames = {"budget_id", "category"}))
public class BudgetCategory extends BaseEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ExpenseCategory category;

    @NotNull
    @Column(name = "spending_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal spendingLimit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_budget_categories_budget"))
    private Budget budget;

    protected BudgetCategory() {
    }

    public BudgetCategory(Budget budget, ExpenseCategory category, BigDecimal spendingLimit) {
        this.budget = budget;
        this.category = category;
        this.spendingLimit = spendingLimit;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public BigDecimal getSpendingLimit() {
        return spendingLimit;
    }

    public Budget getBudget() {
        return budget;
    }

    public void update(ExpenseCategory category, BigDecimal spendingLimit) {
        this.category = category;
        this.spendingLimit = spendingLimit;
    }

    public void assignBudget(Budget budget) {
        this.budget = budget;
    }
}
