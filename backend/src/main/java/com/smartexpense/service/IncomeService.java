package com.smartexpense.service;

import com.smartexpense.config.CurrentUserProvider;
import com.smartexpense.dto.IncomeRequest;
import com.smartexpense.dto.IncomeResponse;
import com.smartexpense.entity.Income;
import com.smartexpense.entity.User;
import com.smartexpense.exception.ResourceNotFoundException;
import com.smartexpense.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final CurrentUserProvider currentUserProvider;

    public IncomeService(IncomeRepository incomeRepository, CurrentUserProvider currentUserProvider) {
        this.incomeRepository = incomeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public IncomeResponse create(IncomeRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Income income = new Income(user, request.amount(), request.date(), request.source(), request.description());
        return toResponse(incomeRepository.save(income));
    }

    @Transactional(readOnly = true)
    public List<IncomeResponse> findAll() {
        return incomeRepository.findAllByUserIdOrderByIncomeDateDesc(currentUserProvider.getCurrentUser().getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public IncomeResponse findById(UUID id) {
        return toResponse(findOwned(id));
    }

    public IncomeResponse update(UUID id, IncomeRequest request) {
        Income income = findOwned(id);
        income.update(request.amount(), request.date(), request.source(), request.description());
        return toResponse(incomeRepository.save(income));
    }

    public void delete(UUID id) {
        incomeRepository.delete(findOwned(id));
    }

    private Income findOwned(UUID id) {
        return incomeRepository.findByIdAndUserId(id, currentUserProvider.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Income not found"));
    }

    private IncomeResponse toResponse(Income income) {
        return new IncomeResponse(income.getId(), income.getAmount(), income.getSource(), income.getIncomeDate(),
                income.getDescription(), income.getCreatedAt(), income.getUpdatedAt());
    }
}
