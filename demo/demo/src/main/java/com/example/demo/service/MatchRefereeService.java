package com.example.demo.service;

import com.example.demo.dao.RefereeRepository;
import com.example.demo.dao.match.MatchRefereeRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dto.matchreferee.MatchRefereeAssignRequest;
import com.example.demo.dto.matchreferee.MatchRefereeResponse;
import com.example.demo.entity.Match;
import com.example.demo.entity.MatchReferee;
import com.example.demo.entity.Referee;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchRefereeService {

    private static final String MAIN_REFEREE = "MAIN_REFEREE";

    private final MatchRefereeRepository matchRefereeRepository;
    private final MatchRepository matchRepository;
    private final RefereeRepository refereeRepository;

    @Transactional
    public MatchRefereeResponse assign(MatchRefereeAssignRequest request) {
        validateAssignRequest(request);

        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu id = " + request.getMatchId()));

        Referee referee = refereeRepository.findById(request.getRefereeId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trọng tài id = " + request.getRefereeId()));

        String normalizedRole = normalizeRole(request.getRole());

        if (matchRefereeRepository.existsByMatchIdAndRefereeId(match.getId(), referee.getId())) {
            throw new RuntimeException("Trọng tài này đã được phân công trong trận đấu này");
        }

        if (isMainRefereeRole(normalizedRole)
                && matchRefereeRepository.existsByMatchIdAndRoleIgnoreCase(match.getId(), MAIN_REFEREE)) {
            throw new RuntimeException("Trận đấu đã có trọng tài chính");
        }

        if (match.getMatchDate() != null
                && matchRefereeRepository.existsRefereeAssignmentAtSameTime(
                referee.getId(),
                match.getMatchDate(),
                null
        )) {
            throw new RuntimeException("Trọng tài đã được phân công ở một trận khác cùng thời điểm");
        }

        MatchReferee assignment = new MatchReferee();
        assignment.setMatch(match);
        assignment.setReferee(referee);
        assignment.setRole(normalizedRole);

        MatchReferee saved = matchRefereeRepository.save(assignment);

        return toResponse(saved);
    }

    public List<MatchRefereeResponse> getByMatch(Long matchId) {
        if (matchId == null || matchId <= 0) {
            throw new RuntimeException("matchId không hợp lệ");
        }

        if (!matchRepository.existsById(matchId)) {
            throw new RuntimeException("Không tìm thấy trận đấu id = " + matchId);
        }

        return matchRefereeRepository.findByMatchId(matchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void remove(Long id) {
        if (id == null || id <= 0) {
            throw new RuntimeException("id phân công trọng tài không hợp lệ");
        }

        MatchReferee assignment = matchRefereeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân công trọng tài id = " + id));

        matchRefereeRepository.delete(assignment);
    }

    private void validateAssignRequest(MatchRefereeAssignRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu phân công trọng tài không được để trống");
        }

        if (request.getMatchId() == null || request.getMatchId() <= 0) {
            throw new RuntimeException("Trận đấu không được để trống");
        }

        if (request.getRefereeId() == null || request.getRefereeId() <= 0) {
            throw new RuntimeException("Trọng tài không được để trống");
        }

        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new RuntimeException("Vai trò trọng tài không được để trống");
        }
    }

    private String normalizeRole(String role) {
        String value = role.trim().toUpperCase();

        return switch (value) {
            case "MAIN", "MAIN_REFEREE", "REFEREE", "TRONG_TAI_CHINH", "TRỌNG_TÀI_CHÍNH" -> MAIN_REFEREE;
            case "ASSISTANT", "ASSISTANT_REFEREE", "TRO_LY", "TRỢ_LÝ" -> "ASSISTANT_REFEREE";
            case "FOURTH", "FOURTH_OFFICIAL", "BAN", "TRONG_TAI_BAN", "TRỌNG_TÀI_BÀN" -> "FOURTH_OFFICIAL";
            case "VAR" -> "VAR";
            default -> value;
        };
    }

    private boolean isMainRefereeRole(String role) {
        return MAIN_REFEREE.equalsIgnoreCase(role);
    }

    private MatchRefereeResponse toResponse(MatchReferee assignment) {
        Referee referee = assignment.getReferee();

        return new MatchRefereeResponse(
                assignment.getId(),
                assignment.getMatch() != null ? assignment.getMatch().getId() : null,
                referee != null ? referee.getId() : null,
                referee != null ? referee.getName() : null,
                referee != null ? referee.getNationality() : null,
                assignment.getRole()
        );
    }
}
