package com.example.demo.controller;

import com.example.demo.dto.systemrule.SystemRuleRequest;
import com.example.demo.dto.systemrule.SystemRuleResponse;
import com.example.demo.service.SystemRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system-rules")
@RequiredArgsConstructor
@CrossOrigin
public class SystemRuleController {

    private final SystemRuleService systemRuleService;

    @GetMapping
    public Page<SystemRuleResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return systemRuleService.getAll(PageRequest.of(page, size));
    }

    @GetMapping("/all")
    public List<SystemRuleResponse> getAllNoPaging() {
        return systemRuleService.getAllNoPaging();
    }

    @GetMapping("/{id}")
    public SystemRuleResponse getById(@PathVariable Long id) {
        return systemRuleService.getById(id);
    }

    @PostMapping
    public SystemRuleResponse create(@RequestBody SystemRuleRequest request) {
        return systemRuleService.create(request);
    }

    @PutMapping("/{id}")
    public SystemRuleResponse update(
            @PathVariable Long id,
            @RequestBody SystemRuleRequest request
    ) {
        return systemRuleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        systemRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
