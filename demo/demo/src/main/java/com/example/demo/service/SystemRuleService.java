package com.example.demo.service;

import com.example.demo.controller.SystemRuleController;
import com.example.demo.dao.SystemRuleRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dto.systemrule.SystemRuleRequest;
import com.example.demo.dto.systemrule.SystemRuleResponse;
import com.example.demo.entity.GoalType;
import com.example.demo.entity.SystemRule;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class SystemRuleService {

    private final SystemRuleRepository systemRuleRepository;
    private final SeasonRepository seasonRepository;
    public Page<SystemRuleResponse> getAll(Pageable pageable) {
        return systemRuleRepository.findAll(pageable).map(this::toResponse);
    }

    public List<SystemRuleResponse> getAllNoPaging() {
        return systemRuleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SystemRuleResponse getById(Long id) {
        SystemRule rule = systemRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ luật id = " + id));

        return toResponse(rule);
    }

    @Transactional
    public SystemRuleResponse create(SystemRuleRequest request) {
        validateRequest(request);

        if (systemRuleRepository.existsByRuleNameIgnoreCase(request.getRuleName())) {
            throw new RuntimeException("Tên bộ luật đã tồn tại");
        }

        SystemRule rule = new SystemRule();
        applyRequest(rule, request);

        SystemRule saved = systemRuleRepository.save(rule);
        return toResponse(saved);
    }

    @Transactional
    public SystemRuleResponse update(Long id, SystemRuleRequest request) {
        validateRequest(request);

        SystemRule rule = systemRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ luật id = " + id));

        if (systemRuleRepository.existsByRuleNameIgnoreCaseAndIdNot(request.getRuleName(), id)) {
            throw new RuntimeException("Tên bộ luật đã tồn tại");
        }

        applyRequest(rule, request);

        SystemRule saved = systemRuleRepository.save(rule);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!systemRuleRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy bộ luật id = " + id);
        }

        if (seasonRepository.existsBySystemRuleId(id)) {
            throw new RuntimeException("Không thể xóa bộ luật đang được áp dụng cho mùa giải");
        }

        systemRuleRepository.deleteById(id);
    }

    private void applyRequest(SystemRule rule, SystemRuleRequest request) {
        rule.setRuleName(request.getRuleName());
        rule.setDescription(request.getDescription());

        rule.setMaxTeams(request.getMaxTeams());
        rule.setMinAge(request.getMinAge());
        rule.setMaxAge(request.getMaxAge());

        rule.setMinPlayers(request.getMinPlayers());
        rule.setMaxPlayers(request.getMaxPlayers());

        rule.setWinPoints(request.getWinPoints());
        rule.setDrawPoints(request.getDrawPoints());
        rule.setLosePoints(request.getLosePoints());

        rule.setAllowedGoalTypes(request.getAllowedGoalTypes());
        rule.setStatus(
                request.getStatus() == null || request.getStatus().isBlank()
                        ? "ACTIVE"
                        : request.getStatus().trim().toUpperCase()
        );

        rule.setMaxSubstitution(request.getMaxSubstitution());
        rule.setMinRegistrationPlayers(request.getMinRegistrationPlayers());
        rule.setMaxForeignPlayers(request.getMaxForeignPlayers());
    }

    private void validateRequest(SystemRuleRequest request) {

        if (request == null) {
            throw new RuntimeException("Dữ liệu bộ luật không được để trống");
        }

        if (request.getRuleName() == null || request.getRuleName().isBlank()) {
            throw new RuntimeException("Tên bộ luật không được để trống");
        }
        if (request.getMaxTeams() == null || request.getMaxTeams() <= 0) {
            throw new RuntimeException("Số đội tối đa phải lớn hơn 0");
        }

        if (request.getMinAge() != null
                && request.getMaxAge() != null
                && request.getMinAge() > request.getMaxAge()) {
            throw new RuntimeException("Tuổi tối thiểu không được lớn hơn tuổi tối đa");
        }

        if (request.getMinPlayers() != null
                && request.getMaxPlayers() != null
                && request.getMinPlayers() > request.getMaxPlayers()) {
            throw new RuntimeException("Số cầu thủ tối thiểu không được lớn hơn tối đa");
        }

        if (request.getMinRegistrationPlayers() != null
                && request.getMaxPlayers() != null
                && request.getMinRegistrationPlayers() > request.getMaxPlayers()) {
            throw new RuntimeException("Số cầu thủ đăng ký tối thiểu không được lớn hơn số cầu thủ tối đa");
        }

        if (request.getMaxForeignPlayers() != null
                && request.getMaxPlayers() != null
                && request.getMaxForeignPlayers() > request.getMaxPlayers()) {
            throw new RuntimeException("Số ngoại binh không được lớn hơn tổng số cầu thủ tối đa");
        }

        if (request.getWinPoints() != null
                && request.getDrawPoints() != null
                && request.getWinPoints() <= request.getDrawPoints()) {
            throw new RuntimeException("Điểm thắng nên lớn hơn điểm hòa");
        }

        if (request.getLosePoints() != null && request.getLosePoints() < 0) {
            throw new RuntimeException("Điểm thua không được âm");
        }

        if (request.getMaxSubstitution() != null && request.getMaxSubstitution() < 0) {
            throw new RuntimeException("Số lượt thay người không được âm");
        }
        if (request.getMinAge() != null && request.getMinAge() < 0) {
            throw new RuntimeException("Tuổi tối thiểu không được âm");
        }

        if (request.getMaxAge() != null && request.getMaxAge() < 0) {
            throw new RuntimeException("Tuổi tối đa không được âm");
        }

        if (request.getMinPlayers() != null && request.getMinPlayers() < 0) {
            throw new RuntimeException("Số cầu thủ tối thiểu không được âm");
        }

        if (request.getMaxPlayers() != null && request.getMaxPlayers() < 0) {
            throw new RuntimeException("Số cầu thủ tối đa không được âm");
        }

        if (request.getMinRegistrationPlayers() != null && request.getMinRegistrationPlayers() < 0) {
            throw new RuntimeException("Số cầu thủ đăng ký tối thiểu không được âm");
        }

        if (request.getMaxForeignPlayers() != null && request.getMaxForeignPlayers() < 0) {
            throw new RuntimeException("Số ngoại binh không được âm");
        }

        if (request.getWinPoints() != null && request.getWinPoints() < 0) {
            throw new RuntimeException("Điểm thắng không được âm");
        }

        if (request.getDrawPoints() != null && request.getDrawPoints() < 0) {
            throw new RuntimeException("Điểm hòa không được âm");
        }

        if (request.getStatus() != null
                && !request.getStatus().isBlank()
                && !request.getStatus().equalsIgnoreCase("ACTIVE")
                && !request.getStatus().equalsIgnoreCase("INACTIVE")) {
            throw new RuntimeException("Trạng thái bộ luật không hợp lệ");
        }

        validateAllowedGoalTypes(request.getAllowedGoalTypes());
    }
    private void validateAllowedGoalTypes(String allowedGoalTypes) {
        if (allowedGoalTypes == null || allowedGoalTypes.isBlank()) {
            return;
        }

        for (String rawType : allowedGoalTypes.split(",")) {
            String type = rawType.trim();

            if (type.isBlank()) {
                continue;
            }

            try {
                GoalType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException("Loại bàn thắng không hợp lệ: " + type);
            }
        }
    }

    private String normalizeAllowedGoalTypes(String allowedGoalTypes) {
        if (allowedGoalTypes == null || allowedGoalTypes.isBlank()) {
            return null;
        }

        return java.util.Arrays.stream(allowedGoalTypes.split(","))
                .map(String::trim)
                .filter(type -> !type.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
    }
    private SystemRuleResponse toResponse(SystemRule rule) {
        return new SystemRuleResponse(
                rule.getId(),
                rule.getRuleName(),
                rule.getDescription(),
                rule.getMaxTeams(),
                rule.getMinAge(),
                rule.getMaxAge(),
                rule.getMinPlayers(),
                rule.getMaxPlayers(),
                rule.getWinPoints(),
                rule.getDrawPoints(),
                rule.getLosePoints(),
                rule.getAllowedGoalTypes(),
                rule.getStatus(),
                rule.getMaxSubstitution(),
                rule.getMinRegistrationPlayers(),
                rule.getMaxForeignPlayers()
        );
    }
}
