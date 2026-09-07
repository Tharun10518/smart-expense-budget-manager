package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.IncomeRequest;
import com.smartexpense.dto.IncomeResponse;
import com.smartexpense.entity.Income;
import com.smartexpense.entity.IncomeSource;
import com.smartexpense.entity.User;
import com.smartexpense.exception.ResourceNotFoundException;
import com.smartexpense.repository.IncomeRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncomeServiceTest {

    @Mock private IncomeRepository incomeRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private User user;

    private final UUID userId = UUID.randomUUID();
    private final UUID incomeId = UUID.randomUUID();
    private IncomeService incomeService;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void setUp() {
        incomeService = new IncomeService(incomeRepository, currentUserProvider);
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(user);
        lenient().when(user.getId()).thenReturn(userId);
    }

    @Test
    void createIncomeSavesAndReturnsResponse() {
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncomeResponse response = incomeService.create(request());

        assertEquals(request().amount(), response.amount());
        assertEquals(request().source(), response.source());
        verify(incomeRepository).save(any(Income.class));
    }

    @Test
    void listReturnsOnlyRepositoryOwnedRecords() {
        when(incomeRepository.findAllByUserIdOrderByIncomeDateDesc(userId)).thenReturn(List.of(income()));

        assertEquals(1, incomeService.findAll().size());
        verify(incomeRepository).findAllByUserIdOrderByIncomeDateDesc(userId);
    }

    @Test
    void getIncomeReturnsOwnedRecord() {
        when(incomeRepository.findByIdAndUserId(incomeId, userId)).thenReturn(Optional.of(income()));

        assertEquals(income().getAmount(), incomeService.findById(incomeId).amount());
    }

    @Test
    void updateIncomeMutatesAndSavesRecord() {
        Income existing = income();
        when(incomeRepository.findByIdAndUserId(incomeId, userId)).thenReturn(Optional.of(existing));
        when(incomeRepository.save(existing)).thenReturn(existing);

        IncomeResponse response = incomeService.update(incomeId, request());

        assertEquals(request().description(), response.description());
        verify(incomeRepository).save(existing);
    }

    @Test
    void deleteIncomeDeletesOwnedRecord() {
        Income existing = income();
        when(incomeRepository.findByIdAndUserId(incomeId, userId)).thenReturn(Optional.of(existing));

        incomeService.delete(incomeId);

        verify(incomeRepository).delete(existing);
    }

    @Test
    void anotherUsersIncomeIsNotAccessible() {
        when(incomeRepository.findByIdAndUserId(incomeId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> incomeService.findById(incomeId));
    }

    @Test
    void invalidAmountFailsRequestValidation() {
        IncomeRequest invalid = new IncomeRequest(BigDecimal.ZERO, IncomeSource.SALARY, LocalDate.now(), "Invalid");

        assertEquals(1, validator.validate(invalid).size());
    }

    private IncomeRequest request() {
        return new IncomeRequest(new BigDecimal("2500.00"), IncomeSource.SALARY, LocalDate.of(2026, 9, 1), "Monthly salary");
    }

    private Income income() {
        return new Income(user, new BigDecimal("2500.00"), LocalDate.of(2026, 9, 1), IncomeSource.SALARY, "Monthly salary");
    }
}
