package com.smartexpense.repository;

import com.smartexpense.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IncomeRepository extends JpaRepository<Income, UUID> {

	List<Income> findAllByUserIdOrderByIncomeDateDesc(UUID userId);

	java.util.Optional<Income> findByIdAndUserId(UUID id, UUID userId);
}
