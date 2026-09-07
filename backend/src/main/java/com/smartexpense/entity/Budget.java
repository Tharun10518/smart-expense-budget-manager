package com.smartexpense.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "budgets", uniqueConstraints = @UniqueConstraint(name = "uk_budget_user_period", columnNames = {"user_id", "budget_year", "budget_month"}))
public class Budget extends BaseEntity {

    @NotNull
    @Column(name = "budget_year", nullable = false)
    private Integer year;

    @NotNull
    @Column(name = "budget_month", nullable = false)
    private Integer month;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 20)
    private BudgetPeriod period = BudgetPeriod.MONTHLY;

    @NotNull
    @Column(name = "total_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalLimit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_budgets_user"))
    private User user;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetCategory> categories = new ArrayList<>();

    protected Budget() {
    }

    public Budget(User user, Integer year, Integer month, BigDecimal totalLimit) {
        this.user = user;
        this.year = year;
        this.month = month;
        this.totalLimit = totalLimit;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public BudgetPeriod getPeriod() {
        return period;
    }

    public BigDecimal getTotalLimit() {
        return totalLimit;
    }

    public User getUser() {
        return user;
    }

    public List<BudgetCategory> getCategories() {
        return categories;
    }

    public void update(Integer year, Integer month, BigDecimal totalLimit) {
        this.year = year;
        this.month = month;
        this.totalLimit = totalLimit;
    }

    public void recalculateTotal() {
        this.totalLimit = categories.stream().map(BudgetCategory::getSpendingLimit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
