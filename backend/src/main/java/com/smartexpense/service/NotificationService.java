package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.NotificationResponse;
import com.smartexpense.entity.Budget;
import com.smartexpense.entity.BudgetCategory;
import com.smartexpense.entity.Expense;
import com.smartexpense.entity.Income;
import com.smartexpense.entity.Notification;
import com.smartexpense.entity.NotificationStatus;
import com.smartexpense.entity.NotificationType;
import com.smartexpense.entity.User;
import com.smartexpense.exception.ResourceNotFoundException;
import com.smartexpense.repository.BudgetRepository;
import com.smartexpense.repository.ExpenseRepository;
import com.smartexpense.repository.IncomeRepository;
import com.smartexpense.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CurrentUserProvider currentUserProvider;

    public NotificationService(NotificationRepository notificationRepository,
                               BudgetRepository budgetRepository,
                               ExpenseRepository expenseRepository,
                               IncomeRepository incomeRepository,
                               CurrentUserProvider currentUserProvider) {
        this.notificationRepository = notificationRepository;
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<NotificationResponse> findAll() {
        User user = currentUserProvider.getCurrentUser();
        syncDerivedAlerts(user);
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    public NotificationResponse markAsRead(UUID id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, currentUserProvider.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.markAsRead(Instant.now());
        return toResponse(notificationRepository.save(notification));
    }

    public void markAllAsRead() {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        List<Notification> unread = notificationRepository.findAllByUserIdAndStatus(userId, NotificationStatus.UNREAD);
        if (!unread.isEmpty()) {
            Instant now = Instant.now();
            unread.forEach(n -> n.markAsRead(now));
            notificationRepository.saveAll(unread);
        }
    }

    private void syncDerivedAlerts(User user) {
        List<Notification> existing = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());

        // 1. Initial welcome / system notification if no notifications exist for the user
        boolean hasWelcome = existing.stream()
                .anyMatch(n -> n.getType() == NotificationType.SYSTEM && n.getTitle().contains("Welcome"));
        if (!hasWelcome) {
            Notification welcome = new Notification(
                    user,
                    NotificationType.SYSTEM,
                    "Welcome to Ledgerly",
                    "Welcome to Smart Expense & Budget Manager. Start tracking your income and expenses to unlock real-time financial insights."
            );
            notificationRepository.save(welcome);
        }

        // 2. Budget Alerts for the current month
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        budgetRepository.findByUserIdAndYearAndMonth(user.getId(), currentYear, currentMonth).ifPresent(budget -> {
            List<Expense> monthlyExpenses = expenseRepository.findAllByUserIdOrderByExpenseDateDesc(user.getId()).stream()
                    .filter(e -> e.getExpenseDate() != null
                            && e.getExpenseDate().getYear() == currentYear
                            && e.getExpenseDate().getMonthValue() == currentMonth)
                    .toList();

            for (BudgetCategory cat : budget.getCategories()) {
                BigDecimal spent = monthlyExpenses.stream()
                        .filter(e -> e.getCategory() == cat.getCategory())
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal limit = cat.getSpendingLimit();

                if (limit != null && limit.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percentage = spent.multiply(BigDecimal.valueOf(100)).divide(limit, 0, RoundingMode.HALF_UP);
                    if (spent.compareTo(limit) > 0) {
                        String exceededTitle = "Budget Exceeded: " + cat.getCategory().name() + " (" + currentMonth + "/" + currentYear + ")";
                        boolean exists = existing.stream().anyMatch(n -> exceededTitle.equals(n.getTitle()));
                        if (!exists) {
                            Notification alert = new Notification(
                                    user,
                                    NotificationType.BUDGET_ALERT,
                                    exceededTitle,
                                    "You have exceeded your " + cat.getCategory().name() + " monthly budget of ₹" + limit.toPlainString() + " (Spent: ₹" + spent.toPlainString() + ")."
                            );
                            notificationRepository.save(alert);
                        }
                    } else if (percentage.compareTo(BigDecimal.valueOf(75)) >= 0) {
                        String warningTitle = "Budget Warning: " + cat.getCategory().name() + " (" + currentMonth + "/" + currentYear + ")";
                        boolean exists = existing.stream().anyMatch(n -> warningTitle.equals(n.getTitle()));
                        if (!exists) {
                            Notification alert = new Notification(
                                    user,
                                    NotificationType.BUDGET_ALERT,
                                    warningTitle,
                                    "You have reached " + percentage + "% of your " + cat.getCategory().name() + " monthly budget (Spent: ₹" + spent.toPlainString() + " of ₹" + limit.toPlainString() + ")."
                            );
                            notificationRepository.save(alert);
                        }
                    }
                }
            }
        });

        // 3. High Spending Alert (monthly expenses > monthly income)
        List<Expense> allCurrentExpenses = expenseRepository.findAllByUserIdOrderByExpenseDateDesc(user.getId()).stream()
                .filter(e -> e.getExpenseDate() != null
                        && e.getExpenseDate().getYear() == currentYear
                        && e.getExpenseDate().getMonthValue() == currentMonth)
                .toList();
        List<Income> allCurrentIncome = incomeRepository.findAllByUserIdOrderByIncomeDateDesc(user.getId()).stream()
                .filter(i -> i.getIncomeDate() != null
                        && i.getIncomeDate().getYear() == currentYear
                        && i.getIncomeDate().getMonthValue() == currentMonth)
                .toList();

        BigDecimal totalExpenses = allCurrentExpenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIncome = allCurrentIncome.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalExpenses.compareTo(BigDecimal.ZERO) > 0 && totalIncome.compareTo(BigDecimal.ZERO) > 0 && totalExpenses.compareTo(totalIncome) > 0) {
            String spendingTitle = "High Spending Notice: " + currentMonth + "/" + currentYear;
            boolean exists = existing.stream().anyMatch(n -> spendingTitle.equals(n.getTitle()));
            if (!exists) {
                Notification alert = new Notification(
                        user,
                        NotificationType.SPENDING_INSIGHT,
                        spendingTitle,
                        "Your total expenses (₹" + totalExpenses.toPlainString() + ") have exceeded your total income (₹" + totalIncome.toPlainString() + ") for this month."
                );
                notificationRepository.save(alert);
            }
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(), notification.getMessage(),
                notification.getStatus(), notification.getReadAt(), notification.getCreatedAt(), notification.getUpdatedAt());
    }
}
