package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.DashboardResponse;
import com.smartexpense.entity.Budget;
import com.smartexpense.entity.Expense;
import com.smartexpense.entity.Income;
import com.smartexpense.repository.BudgetRepository;
import com.smartexpense.repository.ExpenseRepository;
import com.smartexpense.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;
    private final CurrentUserProvider currentUserProvider;

    public DashboardService(ExpenseRepository expenseRepository, IncomeRepository incomeRepository,
                            BudgetRepository budgetRepository, CurrentUserProvider currentUserProvider) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.budgetRepository = budgetRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        List<Expense> expenses = expenseRepository.findAllByUserIdOrderByExpenseDateDesc(currentUserProvider.getCurrentUser().getId());
        List<Income> income = incomeRepository.findAllByUserIdOrderByIncomeDateDesc(currentUserProvider.getCurrentUser().getId());
        List<Budget> budgets = budgetRepository.findAllByUserIdOrderByYearDescMonthDesc(currentUserProvider.getCurrentUser().getId());

        BigDecimal totalIncome = sumIncome(income);
        BigDecimal totalExpenses = sumExpenses(expenses);
        BigDecimal monthlyIncome = income.stream().filter(item -> isCurrentMonth(item.getIncomeDate(), today)).map(Income::getAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal monthlyExpenses = expenses.stream().filter(item -> isCurrentMonth(item.getExpenseDate(), today)).map(Expense::getAmount).reduce(ZERO, BigDecimal::add);
        LocalDate previousMonthDate = today.minusMonths(1);
        BigDecimal previousMonthIncome = income.stream().filter(item -> isMonth(item.getIncomeDate(), previousMonthDate)).map(Income::getAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal previousMonthExpenses = expenses.stream().filter(item -> isMonth(item.getExpenseDate(), previousMonthDate)).map(Expense::getAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal monthlySavings = monthlyIncome.subtract(monthlyExpenses);
        BigDecimal monthlySavingsPercentage = monthlyIncome.signum() == 0 ? ZERO : monthlySavings.multiply(BigDecimal.valueOf(100)).divide(monthlyIncome, 2, RoundingMode.HALF_UP);
        Budget budget = budgets.stream().filter(item -> item.getYear() == today.getYear() && item.getMonth() == today.getMonthValue()).findFirst().orElse(null);
        BigDecimal budgetTotal = budget == null ? ZERO : budget.getTotalLimit();
        BigDecimal budgetRemaining = budgetTotal.subtract(monthlyExpenses);
        BigDecimal percentageUsed = budgetTotal.signum() == 0 ? ZERO : monthlyExpenses.multiply(BigDecimal.valueOf(100)).divide(budgetTotal, 2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        expenses.stream().filter(item -> isCurrentMonth(item.getExpenseDate(), today)).forEach(item -> categoryTotals.merge(item.getCategory().name(), item.getAmount(), BigDecimal::add));
        List<DashboardResponse.Transaction> recentTransactions = recentTransactions(expenses, income);

        return new DashboardResponse(totalIncome, totalExpenses, totalIncome.subtract(totalExpenses), budgetRemaining,
                monthlyIncome, monthlyExpenses, monthlyIncome.subtract(monthlyExpenses),
                previousMonthIncome, previousMonthExpenses, monthlySavingsPercentage,
                (int) (income.stream().filter(item -> isCurrentMonth(item.getIncomeDate(), today)).count()
                        + expenses.stream().filter(item -> isCurrentMonth(item.getExpenseDate(), today)).count()),
                recentTransactions,
                categoryTotals.entrySet().stream().map(item -> new DashboardResponse.CategoryTotal(item.getKey(), item.getValue())).toList(),
                monthlyTrend(expenses, income),
                new DashboardResponse.BudgetSummary(budgetTotal, monthlyExpenses, budgetRemaining, percentageUsed, budgetStatus(percentageUsed)));
    }

            private List<DashboardResponse.MonthlyTotal> monthlyTrend(List<Expense> expenses, List<Income> income) {
            Map<YearMonth, BigDecimal[]> totals = new java.util.TreeMap<>();
            expenses.forEach(item -> totals.computeIfAbsent(YearMonth.from(item.getExpenseDate()), ignored -> new BigDecimal[]{ZERO, ZERO})[1] =
                totals.get(YearMonth.from(item.getExpenseDate()))[1].add(item.getAmount()));
            income.forEach(item -> totals.computeIfAbsent(YearMonth.from(item.getIncomeDate()), ignored -> new BigDecimal[]{ZERO, ZERO})[0] =
                totals.get(YearMonth.from(item.getIncomeDate()))[0].add(item.getAmount()));
            return totals.entrySet().stream().sorted(Map.Entry.<YearMonth, BigDecimal[]>comparingByKey().reversed()).limit(6).sorted(Map.Entry.comparingByKey())
                .map(item -> new DashboardResponse.MonthlyTotal(item.getKey().toString(), item.getValue()[0], item.getValue()[1])).toList();
            }

    private List<DashboardResponse.Transaction> recentTransactions(List<Expense> expenses, List<Income> income) {
        List<DashboardResponse.Transaction> transactions = new ArrayList<>();
        expenses.forEach(item -> transactions.add(new DashboardResponse.Transaction("EXPENSE", item.getCategory().name(), item.getDescription(), item.getExpenseDate(), item.getAmount())));
        income.forEach(item -> transactions.add(new DashboardResponse.Transaction("INCOME", item.getSource().name(), item.getDescription(), item.getIncomeDate(), item.getAmount())));
        return transactions.stream().sorted(Comparator.comparing(DashboardResponse.Transaction::date).reversed()).limit(8).toList();
    }

    private BigDecimal sumExpenses(List<Expense> expenses) {
        return expenses.stream().map(Expense::getAmount).reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal sumIncome(List<Income> income) {
        return income.stream().map(Income::getAmount).reduce(ZERO, BigDecimal::add);
    }

    private boolean isCurrentMonth(LocalDate date, LocalDate today) {
        return date != null && date.getYear() == today.getYear() && date.getMonth() == today.getMonth();
    }

    private boolean isMonth(LocalDate date, LocalDate target) {
        return date != null && date.getYear() == target.getYear() && date.getMonth() == target.getMonth();
    }

    private String budgetStatus(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.valueOf(100)) >= 0) return "EXCEEDED";
        if (percentage.compareTo(BigDecimal.valueOf(75)) >= 0) return "WARNING";
        return "ON_TRACK";
    }
}