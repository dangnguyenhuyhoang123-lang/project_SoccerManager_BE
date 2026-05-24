package com.example.demo.service;

import com.example.demo.controller.SystemRuleController;
import com.example.demo.dao.SystemRuleRepository;
import com.example.demo.entity.SystemRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SystemRuleService {

    private final SystemRuleRepository systemRuleRepository;

    @Autowired
    public SystemRuleService(SystemRuleRepository systemRuleRepository) {
        this.systemRuleRepository = systemRuleRepository;
    }

    public Page<SystemRuleController.SystemRuleResponse> getSystemRules(int page, int size) {
        return systemRuleRepository.findAll(PageRequest.of(page, Math.min(size, 100)))
                .map(this::toResponse);
    }

    public SystemRuleController.SystemRuleResponse getSystemRule(Long id) {
        return toResponse(findEntity(id));
    }

    public SystemRuleController.SystemRuleResponse create(SystemRuleController.SystemRuleRequest request) {
        SystemRule systemRule = new SystemRule();
        applyRequest(systemRule, request);
        return toResponse(systemRuleRepository.save(systemRule));
    }

    public SystemRuleController.SystemRuleResponse update(Long id, SystemRuleController.SystemRuleRequest request) {
        SystemRule systemRule = findEntity(id);
        applyRequest(systemRule, request);
        return toResponse(systemRuleRepository.save(systemRule));
    }

    public void delete(Long id) {
        if (!systemRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("System rule not found with id = " + id);
        }
        systemRuleRepository.deleteById(id);
    }

    private SystemRule findEntity(Long id) {
        return systemRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("System rule not found with id = " + id));
    }

    private void applyRequest(SystemRule systemRule, SystemRuleController.SystemRuleRequest request) {
        systemRule.setMaxTeams(request.maxTeams());
        systemRule.setMinAge(request.minAge());
        systemRule.setMaxAge(request.maxAge());
        systemRule.setMinPlayers(request.minPlayers());
        systemRule.setMaxPlayers(request.maxPlayers());
        systemRule.setWinPoints(request.winPoints());
        systemRule.setDrawPoints(request.drawPoints());
        systemRule.setLosePoints(request.losePoints());
        systemRule.setAllowedGoalTypes(request.allowedGoalTypes());
        systemRule.setStatus(request.status());
        systemRule.setMaxSubstitution(request.maxSubstitution());
        systemRule.setMinRegistrationPlayers(request.minRegistrationPlayers());
        systemRule.setMaxForeignPlayers(request.maxForeignPlayers());
    }

    private SystemRuleController.SystemRuleResponse toResponse(SystemRule systemRule) {
        return new SystemRuleController.SystemRuleResponse(
                systemRule.getId(),
                systemRule.getMaxTeams(),
                systemRule.getMinAge(),
                systemRule.getMaxAge(),
                systemRule.getMinPlayers(),
                systemRule.getMaxPlayers(),
                systemRule.getWinPoints(),
                systemRule.getDrawPoints(),
                systemRule.getLosePoints(),
                systemRule.getAllowedGoalTypes(),
                systemRule.getStatus(),
                systemRule.getMaxSubstitution(),
                systemRule.getMinRegistrationPlayers(),
                systemRule.getMaxForeignPlayers()
        );
    }
}
