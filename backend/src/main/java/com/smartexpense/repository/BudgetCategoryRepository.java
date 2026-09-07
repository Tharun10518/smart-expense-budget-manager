package com.smartexpense.repository;

import com.smartexpense.entity.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, UUID> {

	Optional<BudgetCategory> findByIdAndBudgetUserId(UUID id, UUID userId);
}
