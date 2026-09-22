package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.NotificationResponse;
import com.smartexpense.entity.Budget;
import com.smartexpense.entity.BudgetCategory;
import com.smartexpense.entity.Expense;
import com.smartexpense.entity.ExpenseCategory;
import com.smartexpense.entity.Notification;
import com.smartexpense.entity.NotificationStatus;
import com.smartexpense.entity.NotificationType;
import com.smartexpense.entity.PaymentMethod;
import com.smartexpense.entity.User;
import com.smartexpense.repository.BudgetRepository;
import com.smartexpense.repository.ExpenseRepository;
import com.smartexpense.repository.IncomeRepository;
import com.smartexpense.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private User user;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(user);
        lenient().when(user.getId()).thenReturn(userId);
    }

    @Test
    void findAllGeneratesWelcomeNotificationWhenEmpty() {
        when(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(new Notification(user, NotificationType.SYSTEM, "Welcome to Ledgerly", "Welcome!")));

        when(budgetRepository.findByUserIdAndYearAndMonth(any(), any(), any())).thenReturn(Optional.empty());
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)).thenReturn(Collections.emptyList());
        when(incomeRepository.findAllByUserIdOrderByIncomeDateDesc(userId)).thenReturn(Collections.emptyList());

        NotificationService service = new NotificationService(notificationRepository, budgetRepository, expenseRepository, incomeRepository, currentUserProvider);
        List<NotificationResponse> results = service.findAll();

        assertEquals(1, results.size());
        assertEquals("Welcome to Ledgerly", results.get(0).title());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void findAllGeneratesBudgetAlertWhenExceeded() {
        Notification welcome = new Notification(user, NotificationType.SYSTEM, "Welcome to Ledgerly", "Welcome!");
        when(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(new ArrayList<>(List.of(welcome)));

        LocalDate today = LocalDate.now();
        Budget budget = new Budget(user, today.getYear(), today.getMonthValue(), BigDecimal.valueOf(1000));
        BudgetCategory category = new BudgetCategory(budget, ExpenseCategory.FOOD, BigDecimal.valueOf(500));
        budget.getCategories().add(category);

        when(budgetRepository.findByUserIdAndYearAndMonth(userId, today.getYear(), today.getMonthValue()))
                .thenReturn(Optional.of(budget));

        Expense expense = new Expense(user, BigDecimal.valueOf(600), today, ExpenseCategory.FOOD, "Dinner", PaymentMethod.CREDIT_CARD);
        when(expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId))
                .thenReturn(List.of(expense));
        when(incomeRepository.findAllByUserIdOrderByIncomeDateDesc(userId))
                .thenReturn(Collections.emptyList());

        NotificationService service = new NotificationService(notificationRepository, budgetRepository, expenseRepository, incomeRepository, currentUserProvider);
        service.findAll();

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAsReadUpdatesStatus() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(user, NotificationType.SYSTEM, "Test", "Test message");
        when(notificationRepository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationService service = new NotificationService(notificationRepository, budgetRepository, expenseRepository, incomeRepository, currentUserProvider);
        NotificationResponse response = service.markAsRead(notificationId);

        assertEquals(NotificationStatus.READ, response.status());
        assertNotNull(response.readAt());
    }

    @Test
    void markAllAsReadUpdatesAllUnread() {
        Notification n1 = new Notification(user, NotificationType.SYSTEM, "Title 1", "Msg 1");
        Notification n2 = new Notification(user, NotificationType.BUDGET_ALERT, "Title 2", "Msg 2");
        when(notificationRepository.findAllByUserIdAndStatus(userId, NotificationStatus.UNREAD))
                .thenReturn(List.of(n1, n2));

        NotificationService service = new NotificationService(notificationRepository, budgetRepository, expenseRepository, incomeRepository, currentUserProvider);
        service.markAllAsRead();

        assertEquals(NotificationStatus.READ, n1.getStatus());
        assertEquals(NotificationStatus.READ, n2.getStatus());
        verify(notificationRepository).saveAll(any());
    }
}

