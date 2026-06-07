package com.example.demo.service.match;

import com.example.demo.dao.RefereeRepository;
import com.example.demo.dao.match.MatchRefereeRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.dto.matchreferee.MatchRefereeAssignRequest;
import com.example.demo.dto.matchreferee.MatchRefereeResponse;
import com.example.demo.entity.match.Match;
import com.example.demo.entity.match.MatchReferee;
import com.example.demo.entity.Referee;
import com.example.demo.entity.season.SeasonTeam;
import com.example.demo.entity.team.Team;
import com.example.demo.entity.user.User;
import com.example.demo.service.realtime.RealtimeEventService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchRefereeService {

    private final MatchRefereeRepository matchRefereeRepository;
    private final MatchRepository matchRepository;
    private final RefereeRepository refereeRepository;
    private final UserRepository userRepository;
    private final RealtimeEventService realtimeEventService;

    @Transactional
    public MatchRefereeResponse assign(MatchRefereeAssignRequest request) {
        validateAssignRequest(request);

        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu id = " + request.getMatchId()));

        Referee referee = refereeRepository.findById(request.getRefereeId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trọng tài id = " + request.getRefereeId()));

        if (referee.getStatus() != null && !"ACTIVE".equalsIgnoreCase(referee.getStatus())) {
            throw new RuntimeException("Chỉ được phân công trọng tài đang hoạt động");
        }

        String normalizedRole = normalizeRole(request.getRole());

        if (matchRefereeRepository.existsByMatchIdAndRefereeId(match.getId(), referee.getId())) {
            throw new RuntimeException("Trọng tài này đã được phân công trong trận đấu này");
        }

        if (isUniqueRole(normalizedRole)
                && matchRefereeRepository.existsByMatchIdAndRoleIgnoreCase(match.getId(), normalizedRole)) {
            throw new RuntimeException("Trận đấu đã có " + roleLabel(normalizedRole));
        }

        if (match.getMatchDate() != null
                && matchRefereeRepository.existsRefereeAssignmentAtSameTime(referee.getId(), match.getMatchDate(), null)) {
            throw new RuntimeException("Trọng tài đã được phân công ở một trận khác cùng thời điểm");
        }

        MatchReferee assignment = new MatchReferee();
        assignment.setMatch(match);
        assignment.setReferee(referee);
        assignment.setRole(normalizedRole);
        assignment.setNote(trim(request.getNote()));
        assignment.setAssignedAt(LocalDateTime.now());

        MatchReferee saved = matchRefereeRepository.save(assignment);
        sendMatchRefereeRealtimeEvents(match, "MATCH_REFEREE_ASSIGNED");

        return toResponse(saved);
    }

    public List<MatchRefereeResponse> getByMatch(Long matchId) {
        if (matchId == null || matchId <= 0) {
            throw new RuntimeException("matchId không hợp lệ");
        }
        if (!matchRepository.existsById(matchId)) {
            throw new RuntimeException("Không tìm thấy trận đấu id = " + matchId);
        }
        return matchRefereeRepository.findByMatchId(matchId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void remove(Long id) {
        if (id == null || id <= 0) throw new RuntimeException("id phân công trọng tài không hợp lệ");
        MatchReferee assignment = matchRefereeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân công trọng tài id = " + id));
        Match match = assignment.getMatch();
        matchRefereeRepository.delete(assignment);
        sendMatchRefereeRealtimeEvents(match, "MATCH_REFEREE_REMOVED");
    }

    private void sendMatchRefereeRealtimeEvents(Match match, String type) {
        if (match == null || match.getId() == null) {
            return;
        }

        Long matchId = match.getId();
        RealtimeEventDTO event = realtimeEvent(
                type,
                matchId,
                "MATCH_REFEREE",
                "REFETCH_MATCH_REFEREES"
        );

        RealtimeEventDTO detailEvent = realtimeEvent(
                "MATCH_DETAIL_UPDATED",
                matchId,
                "MATCH",
                "REFETCH_MATCH_DETAIL"
        );

        Set<Long> clubManagerUserIds = findRelatedClubManagerUserIds(match);

        realtimeEventService.sendToAdmins(event);
        realtimeEventService.sendToAdmins(detailEvent);
        realtimeEventService.sendToUsers(clubManagerUserIds, event);
        realtimeEventService.sendToUsers(clubManagerUserIds, detailEvent);
        realtimeEventService.sendToPublicMatch(matchId, event);
        realtimeEventService.sendToPublicMatch(matchId, detailEvent);
    }

    private Set<Long> findRelatedClubManagerUserIds(Match match) {
        Set<Long> userIds = new LinkedHashSet<>();

        findClubManagerBySeasonTeam(match.getHomeTeam())
                .map(User::getId)
                .ifPresent(userIds::add);
        findClubManagerBySeasonTeam(match.getAwayTeam())
                .map(User::getId)
                .ifPresent(userIds::add);

        return userIds;
    }

    private Optional<User> findClubManagerBySeasonTeam(SeasonTeam seasonTeam) {
        if (seasonTeam == null || seasonTeam.getTeam() == null) {
            return Optional.empty();
        }

        Team team = seasonTeam.getTeam();

        Optional<User> managerOpt = userRepository.findClubManagerByTeamIdAndRoleName(
                team.getId(),
                "ROLE_CLUB_MANAGER"
        );

        if (managerOpt.isEmpty()) {
            managerOpt = userRepository.findClubManagerByTeamIdAndRoleName(
                    team.getId(),
                    "CLUB_MANAGER"
            );
        }

        if (managerOpt.isEmpty()) {
            managerOpt = userRepository.findFirstByTeamId(team.getId());
        }

        return managerOpt;
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

    private void validateAssignRequest(MatchRefereeAssignRequest request) {
        if (request == null) throw new RuntimeException("Dữ liệu phân công trọng tài không được để trống");
        if (request.getMatchId() == null || request.getMatchId() <= 0) throw new RuntimeException("Trận đấu không được để trống");
        if (request.getRefereeId() == null || request.getRefereeId() <= 0) throw new RuntimeException("Trọng tài không được để trống");
        if (request.getRole() == null || request.getRole().isBlank()) throw new RuntimeException("Vai trò trọng tài không được để trống");
    }

    private String normalizeRole(String role) {
        String value = role.trim().toUpperCase();
        return switch (value) {
            case "MAIN", "MAIN_REFEREE", "REFEREE", "TRONG_TAI_CHINH", "TRỌNG_TÀI_CHÍNH" -> "MAIN_REFEREE";
            case "ASSISTANT_1", "ASSISTANT_REFEREE_1", "TRO_LY_1", "TRỢ_LÝ_1" -> "ASSISTANT_REFEREE_1";
            case "ASSISTANT_2", "ASSISTANT_REFEREE_2", "TRO_LY_2", "TRỢ_LÝ_2" -> "ASSISTANT_REFEREE_2";
            case "ASSISTANT", "ASSISTANT_REFEREE", "TRO_LY", "TRỢ_LÝ" -> "ASSISTANT_REFEREE_1";
            case "FOURTH", "FOURTH_OFFICIAL", "BAN", "TRONG_TAI_BAN", "TRỌNG_TÀI_BÀN" -> "FOURTH_OFFICIAL";
            case "VAR", "VAR_REFEREE" -> "VAR_REFEREE";
            default -> value;
        };
    }

    private boolean isUniqueRole(String role) {
        return List.of("MAIN_REFEREE", "ASSISTANT_REFEREE_1", "ASSISTANT_REFEREE_2", "FOURTH_OFFICIAL", "VAR_REFEREE").contains(role);
    }

    private String roleLabel(String role) {
        return switch (role) {
            case "MAIN_REFEREE" -> "trọng tài chính";
            case "ASSISTANT_REFEREE_1" -> "trợ lý trọng tài 1";
            case "ASSISTANT_REFEREE_2" -> "trợ lý trọng tài 2";
            case "FOURTH_OFFICIAL" -> "trọng tài bàn";
            case "VAR_REFEREE" -> "trọng tài VAR";
            default -> role;
        };
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private MatchRefereeResponse toResponse(MatchReferee assignment) {
        Referee referee = assignment.getReferee();
        return new MatchRefereeResponse(
                assignment.getId(),
                assignment.getMatch() != null ? assignment.getMatch().getId() : null,
                referee != null ? referee.getId() : null,
                referee != null ? referee.getName() : null,
                referee != null ? referee.getNationality() : null,
                assignment.getRole(),
                assignment.getNote()
        );
    }
}
