package com.smartexpense.repository;

import com.smartexpense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

	List<Expense> findAllByUserIdOrderByExpenseDateDesc(UUID userId);

	java.util.Optional<Expense> findByIdAndUserId(UUID id, UUID userId);
}
