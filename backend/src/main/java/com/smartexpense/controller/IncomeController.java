package com.smartexpense.controller;

import com.smartexpense.dto.IncomeRequest;
import com.smartexpense.dto.IncomeResponse;
import com.smartexpense.service.IncomeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/income")
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @PostMapping
    public ResponseEntity<IncomeResponse> create(@Valid @RequestBody IncomeRequest request) {
        IncomeResponse response = incomeService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<IncomeResponse> findAll() {
        return incomeService.findAll();
    }

    @GetMapping("/{id}")
    public IncomeResponse findById(@PathVariable UUID id) {
        return incomeService.findById(id);
    }

    @PutMapping("/{id}")
    public IncomeResponse update(@PathVariable UUID id, @Valid @RequestBody IncomeRequest request) {
        return incomeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        incomeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
