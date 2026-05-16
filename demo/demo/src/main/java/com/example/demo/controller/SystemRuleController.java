package com.example.demo.controller;

import com.example.demo.service.SystemRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system-rules")
@CrossOrigin
public class SystemRuleController {

    private SystemRuleService systemRuleService;

    @Autowired
    public SystemRuleController(SystemRuleService systemRuleService) {
        this.systemRuleService = systemRuleService;
    }

    @GetMapping
    public Page<SystemRuleResponse> getSystemRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return systemRuleService.getSystemRules(page, size);
    }

    @GetMapping("/{id}")
    public SystemRuleResponse getSystemRule(@PathVariable Long id) {
        return systemRuleService.getSystemRule(id);
    }

    @PostMapping
    public SystemRuleResponse createSystemRule(@RequestBody SystemRuleRequest request) {
        return systemRuleService.create(request);
    }

    @PutMapping("/{id}")
    public SystemRuleResponse updateSystemRule(@PathVariable Long id, @RequestBody SystemRuleRequest request) {
        return systemRuleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteSystemRule(@PathVariable Long id) {
        systemRuleService.delete(id);
    }

    public record SystemRuleRequest(
            Integer maxTeams,
            Integer minAge,
            Integer maxAge,
            Integer minPlayers,
            Integer maxPlayers,
            Integer winPoints,
            Integer drawPoints,
            Integer losePoints,
            String allowedGoalTypes,
            String status,
            Integer maxSubstitution,
            Integer minRegistrationPlayers,
            Integer maxForeignPlayers
    ) {
    }

    public record SystemRuleResponse(
            Long id,
            Integer maxTeams,
            Integer minAge,
            Integer maxAge,
            Integer minPlayers,
            Integer maxPlayers,
            Integer winPoints,
            Integer drawPoints,
            Integer losePoints,
            String allowedGoalTypes,
            String status,
            Integer maxSubstitution,
            Integer minRegistrationPlayers,
            Integer maxForeignPlayers
    ) {
    }
}
