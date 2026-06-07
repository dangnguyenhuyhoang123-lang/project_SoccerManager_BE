package com.example.demo.service.season;

import com.example.demo.dao.SystemRuleRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.dto.systemrule.SystemRuleRequest;
import com.example.demo.dto.systemrule.SystemRuleResponse;
import com.example.demo.entity.match.GoalType;
import com.example.demo.entity.SystemRule;
import com.example.demo.service.realtime.RealtimeEventService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SystemRuleService {

    private final SystemRuleRepository systemRuleRepository;
    private final SeasonRepository seasonRepository;
    private final RealtimeEventService realtimeEventService;


    // ==================== QUERY METHODS ====================

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




// ==================== COMMAND METHODS ====================


    @Transactional
    public SystemRuleResponse create(SystemRuleRequest request) {
        validateRequest(request);


        SystemRule rule = new SystemRule();
        applyRequest(rule, request);

        SystemRule saved = systemRuleRepository.save(rule);

        sendSystemRuleRealtimeEvent(saved.getId(), "SYSTEM_RULE_CREATED");
        return toResponse(saved);
    }

    @Transactional
    public SystemRuleResponse update(Long id, SystemRuleRequest request) {
        validateRequest(request);

        SystemRule rule = systemRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ luật id = " + id));



        applyRequest(rule, request);

        SystemRule saved = systemRuleRepository.save(rule);
        sendSystemRuleRealtimeEvent(saved.getId(), "SYSTEM_RULE_UPDATED");
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
        sendSystemRuleRealtimeEvent(id, "SYSTEM_RULE_DELETED");
    }



// ==================== VALIDATION HELPERS ====================
    /**
     * Kiểm tra dữ liệu cấu hình luật mùa giải trước khi tạo hoặc cập nhật.
     *
     * Rule ảnh hưởng đến nhiều nghiệp vụ:
     * - Đăng ký CLB tham gia mùa giải.
     * - Validate cầu thủ, HLV, đội hình thi đấu.
     * - Tính điểm và bảng xếp hạng.
     * - Validate sự kiện bàn thắng hợp lệ.
     */
    private void validateRequest(SystemRuleRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu luật không được để trống");
        }

        validateBasicInfo(request);
        validateTeamLimits(request);
        validatePlayerLimits(request);
        validateAgeLimits(request);
        validatePointRules(request);
        validateSubstitutionRules(request);
        validateForeignPlayerRules(request);
        validateGoalTypeRules(request);
    }


    /**
     * Kiểm tra thông tin cơ bản của bộ luật.
     */
    private void validateBasicInfo(SystemRuleRequest request) {
        if (request.getRuleName() == null || request.getRuleName().isBlank()) {
            throw new RuntimeException("Tên bộ luật không được để trống");
        }

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            throw new RuntimeException("Trạng thái bộ luật không được để trống");
        }

        if (!"ACTIVE".equalsIgnoreCase(request.getStatus())
                && !"INACTIVE".equalsIgnoreCase(request.getStatus())) {
            throw new RuntimeException("Trạng thái bộ luật chỉ được là ACTIVE hoặc INACTIVE");
        }
    }


    /**
     * Kiểm tra giới hạn số đội tham gia mùa giải.
     */
    private void validateTeamLimits(SystemRuleRequest request) {
        if (request.getMaxTeams() == null || request.getMaxTeams() <= 1) {
            throw new RuntimeException("Số đội tối đa phải lớn hơn 1");
        }

        if (request.getMaxTeams() % 2 != 0) {
            throw new RuntimeException("Số đội tối đa nên là số chẵn để thuận tiện sinh lịch thi đấu");
        }
    }


    /**
     * Kiểm tra giới hạn số lượng cầu thủ trong CLB/mùa giải.
     */
    private void validatePlayerLimits(SystemRuleRequest request) {
        if (request.getMinPlayers() == null || request.getMinPlayers() <= 0) {
            throw new RuntimeException("Số cầu thủ tối thiểu phải lớn hơn 0");
        }

        if (request.getMaxPlayers() == null || request.getMaxPlayers() <= 0) {
            throw new RuntimeException("Số cầu thủ tối đa phải lớn hơn 0");
        }

        if (request.getMinPlayers() > request.getMaxPlayers()) {
            throw new RuntimeException("Số cầu thủ tối thiểu không được lớn hơn số cầu thủ tối đa");
        }


    }

    /**
     * Kiểm tra giới hạn độ tuổi cầu thủ.
     */
    private void validateAgeLimits(SystemRuleRequest request) {
        if (request.getMinAge() == null || request.getMinAge() <= 0) {
            throw new RuntimeException("Tuổi tối thiểu phải lớn hơn 0");
        }

        if (request.getMaxAge() == null || request.getMaxAge() <= 0) {
            throw new RuntimeException("Tuổi tối đa phải lớn hơn 0");
        }

        if (request.getMinAge() > request.getMaxAge()) {
            throw new RuntimeException("Tuổi tối thiểu không được lớn hơn tuổi tối đa");
        }
    }

    /**
     * Kiểm tra cấu hình điểm số dùng để tính bảng xếp hạng.
     */
    private void validatePointRules(SystemRuleRequest request) {
        if (request.getWinPoints() == null || request.getWinPoints() < 0) {
            throw new RuntimeException("Điểm thắng không được âm");
        }

        if (request.getDrawPoints() == null || request.getDrawPoints() < 0) {
            throw new RuntimeException("Điểm hòa không được âm");
        }

        if (request.getLosePoints() == null || request.getLosePoints() < 0) {
            throw new RuntimeException("Điểm thua không được âm");
        }

        if (request.getWinPoints() <= request.getDrawPoints()) {
            throw new RuntimeException("Điểm thắng phải lớn hơn điểm hòa");
        }

        if (request.getDrawPoints() < request.getLosePoints()) {
            throw new RuntimeException("Điểm hòa không được nhỏ hơn điểm thua");
        }
    }

    /**
     * Kiểm tra số lượt thay người tối đa trong một trận.
     */
    private void validateSubstitutionRules(SystemRuleRequest request) {
        if (request.getMaxSubstitution() == null) {
            return;
        }

        if (request.getMaxSubstitution() < 0) {
            throw new RuntimeException("Số lượt thay người tối đa không được âm");
        }

        if (request.getMaxSubstitution() > 12) {
            throw new RuntimeException("Số lượt thay người tối đa không nên vượt quá 12");
        }
    }
    /**
     * Kiểm tra giới hạn cầu thủ nước ngoài.
     */
    private void validateForeignPlayerRules(SystemRuleRequest request) {
        if (request.getMaxForeignPlayers() != null && request.getMaxForeignPlayers() < 0) {
            throw new RuntimeException("Số cầu thủ nước ngoài tối đa không được âm");
        }

        if (request.getMaxForeignPlayersOnField() != null && request.getMaxForeignPlayersOnField() < 0) {
            throw new RuntimeException("Số cầu thủ nước ngoài trên sân không được âm");
        }

        if (request.getMaxForeignPlayers() != null
                && request.getMaxForeignPlayersOnField() != null
                && request.getMaxForeignPlayersOnField() > request.getMaxForeignPlayers()) {
            throw new RuntimeException("Số cầu thủ nước ngoài trên sân không được lớn hơn tổng số cầu thủ nước ngoài");
        }
    }

    /**
     * Kiểm tra danh sách loại bàn thắng được phép ghi nhận trong mùa giải.
     * Dữ liệu lưu dạng chuỗi phân tách bởi dấu phẩy, ví dụ: NORMAL,OWN_GOAL,PENALTY.
     */
    private void validateGoalTypeRules(SystemRuleRequest request) {
        String allowedGoalTypes = request.getAllowedGoalTypes();

        if (allowedGoalTypes == null || allowedGoalTypes.isBlank()) {
            throw new RuntimeException("Danh sách loại bàn thắng hợp lệ không được để trống");
        }

        Set<String> validGoalTypes = Set.of("NORMAL", "OWN_GOAL", "PENALTY");

        for (String goalType : allowedGoalTypes.split(",")) {
            String normalizedGoalType = goalType.trim().toUpperCase();

            if (normalizedGoalType.isBlank()) {
                continue;
            }

            if (!validGoalTypes.contains(normalizedGoalType)) {
                throw new RuntimeException("Loại bàn thắng không hợp lệ: " + goalType);
            }
        }
    }


// ==================== MAPPING HELPERS ====================



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

        rule.setAllowedGoalTypes(normalizeAllowedGoalTypes(request.getAllowedGoalTypes()));
        rule.setStatus(
                request.getStatus() == null || request.getStatus().isBlank()
                        ? "ACTIVE"
                        : request.getStatus().trim().toUpperCase()
        );

        rule.setMaxSubstitution(request.getMaxSubstitution());
        rule.setMinCoaches(request.getMinCoaches());
        rule.setMaxCoaches(request.getMaxCoaches());
        rule.setMaxForeignPlayers(request.getMaxForeignPlayers());
        rule.setMaxForeignPlayersOnField(request.getMaxForeignPlayersOnField());
        rule.setMaxGoalMinute(request.getMaxGoalMinute());
        rule.setRankingCriteriaOrder(
                request.getRankingCriteriaOrder() != null && !request.getRankingCriteriaOrder().isBlank()
                        ? request.getRankingCriteriaOrder()
                        : "POINTS,GOAL_DIFFERENCE,GOALS_FOR,HEAD_TO_HEAD,DRAW_LOT"
        );
    }
    private String normalizeAllowedGoalTypes(String allowedGoalTypes) {
        if (allowedGoalTypes == null || allowedGoalTypes.isBlank()) {
            return null;
        }

        return Arrays.stream(allowedGoalTypes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .collect(Collectors.joining(","));
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
                rule.getMinCoaches(),
                rule.getMaxCoaches(),
                rule.getMaxForeignPlayers(),
                rule.getMaxForeignPlayersOnField(),
                rule.getMaxGoalMinute(),
                rule.getRankingCriteriaOrder()

        );
    }


// ==================== REALTIME HELPERS ====================

    private void sendSystemRuleRealtimeEvent(Long ruleId, String type) {
        RealtimeEventDTO event = realtimeEvent(
                type,
                ruleId,
                "SYSTEM_RULE",
                "REFETCH_SYSTEM_RULES"
        );

        realtimeEventService.sendToAdmins(event);
    }

    private RealtimeEventDTO realtimeEvent(
            String type,
            Long referenceId,
            String referenceType,
            String action
    ) {
        return new RealtimeEventDTO(
                type,
                referenceId,
                referenceType,
                action,
                null,
                LocalDateTime.now()
        );
    }





}
