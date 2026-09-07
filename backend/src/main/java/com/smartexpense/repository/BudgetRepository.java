package com.smartexpense.repository;

import com.smartexpense.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

	List<Budget> findAllByUserIdOrderByYearDescMonthDesc(UUID userId);

	java.util.Optional<Budget> findByUserIdAndYearAndMonth(UUID userId, Integer year, Integer month);

	java.util.Optional<Budget> findByIdAndUserId(UUID id, UUID userId);
}
