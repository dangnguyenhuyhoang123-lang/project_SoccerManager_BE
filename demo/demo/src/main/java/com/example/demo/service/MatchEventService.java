package com.example.demo.service;

import com.example.demo.dao.match.MatchEventRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.player.PlayerRepository;
import com.example.demo.dao.player.PlayerSeasonRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.dto.matchevent.MatchEventResponse;
import com.example.demo.dto.matchevent.MatchEventUpsertRequest;
import com.example.demo.entity.*;
import com.example.demo.entity.user.User;
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
public class MatchEventService {

    private final MatchEventRepository matchEventRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerStatsService playerStatsService;
    private final PlayerSeasonRepository playerSeasonRepository;
    private final UserRepository userRepository;
    private final RealtimeEventService realtimeEventService;
    private final StandingService standingService;


    private SystemRule getRequiredRule(Season season) {
        if (season == null) {
            throw new RuntimeException("Không tìm thấy mùa giải");
        }

        SystemRule rule = season.getSystemRule();

        if (rule == null) {
            throw new RuntimeException("Mùa giải chưa được cấu hình bộ luật");
        }

        if (!"ACTIVE".equalsIgnoreCase(rule.getStatus())) {
            throw new RuntimeException("Bộ luật của mùa giải đang tạm ngưng");
        }

        return rule;
    }
    public List<MatchEventResponse> getEventsByMatch(Long matchId) {
        return matchEventRepository.findByMatchIdOrderByMinuteAscExtraMinuteAscEventOrderAscIdAsc(matchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }



    @Transactional
    public MatchEventResponse createEvent(Long matchId, MatchEventUpsertRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu id = " + matchId));

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng id = " + request.getTeamId()));

        SystemRule rule = getRequiredRule(match.getSeason());
        if (request.getEventType() == EventType.GOAL) {
            validateGoalTypeAllowed(rule, request.getGoalType());
        }
        if (request.getEventType() == EventType.SUBSTITUTION) {
            validateSubstitutionLimit(matchId, request.getTeamId(), rule, null);
        }

        validateTeamBelongsToMatch(match, team);
        validateEventRequest(request);

        MatchEvent event = new MatchEvent();
        event.setMatch(match);

        applyRequest(event, request, team);

        MatchEvent savedEvent = matchEventRepository.save(event);



        playerStatsService.applyEvent(savedEvent, 1);
        recalculateMatchScore(matchId);
        sendMatchEventRealtimeEvents(matchId);

        return toResponse(savedEvent);
    }

    private void validateGoalTypeAllowed(SystemRule rule, GoalType goalType) {
        if (goalType == null) {
            return;
        }

        String allowedGoalTypes = rule.getAllowedGoalTypes();

        if (allowedGoalTypes == null || allowedGoalTypes.isBlank()) {
            return;
        }

        boolean allowed = List.of(allowedGoalTypes.split(","))
                .stream()
                .map(String::trim)
                .anyMatch(type -> type.equals(goalType.name()));

        if (!allowed) {
            throw new RuntimeException("Loại bàn thắng " + goalType + " không được áp dụng trong mùa giải này");
        }
    }
    private void validateSubstitutionLimit(
            Long matchId,
            Long teamId,
            SystemRule rule,
            Long currentEventId
    ) {
        if (rule.getMaxSubstitution() == null) {
            return;
        }

        long currentSubCount = matchEventRepository
                .findByMatchId(matchId)
                .stream()
                .filter(e -> e.getEventType() == EventType.SUBSTITUTION)
                .filter(e -> e.getTeam() != null && e.getTeam().getId().equals(teamId))
                .filter(e -> currentEventId == null || !e.getId().equals(currentEventId))
                .count();

        if (currentSubCount >= rule.getMaxSubstitution()) {
            throw new RuntimeException("Đội đã vượt quá số lượt thay người tối đa: " + rule.getMaxSubstitution());
        }
    }
    private void validatePlayerBelongsToTeamInSeason(Player player, Team team, Season season) {
        if (player == null) {
            return;
        }

        boolean exists = playerSeasonRepository.existsByPlayerTeamSeason(
                player.getId(),
                team.getId(),
                season.getId()
        );

        if (!exists) {
            throw new RuntimeException(
                    "Cầu thủ " + player.getName()
                            + " không thuộc đội " + team.getName()
                            + " trong mùa giải này"
            );
        }
    }


    @Transactional
    public MatchEventResponse updateEvent(Long matchId, Long eventId, MatchEventUpsertRequest request) {
        MatchEvent event = matchEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện id = " + eventId));

        if (event.getMatch() == null || !event.getMatch().getId().equals(matchId)) {
            throw new RuntimeException("Sự kiện id = " + eventId + " không thuộc trận đấu id = " + matchId);
        }

        // 1. Trừ thống kê từ event cũ
        playerStatsService.applyEvent(event, -1);

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng id = " + request.getTeamId()));

        validateTeamBelongsToMatch(event.getMatch(), team);
        validateEventRequest(request);

        applyRequest(event, request, team);

        MatchEvent savedEvent = matchEventRepository.save(event);

        // 2. Cộng thống kê từ event mới
        playerStatsService.applyEvent(savedEvent, 1);

        recalculateMatchScore(matchId);
        sendMatchEventRealtimeEvents(matchId);
        return toResponse(savedEvent);
    }

//    @Transactional
//    public void deleteEvent(Long matchId, Long eventId) {
//        MatchEvent event = matchEventRepository.findById(eventId)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện id = " + eventId));
//
//        if (event.getMatch() == null || !event.getMatch().getId().equals(matchId)) {
//            throw new RuntimeException("Sự kiện id = " + eventId + " không thuộc trận đấu id = " + matchId);
//        }
//
//        matchEventRepository.delete(event);
//    }

    @Transactional
    public void deleteEvent(Long matchId, Long eventId) {
        MatchEvent event = matchEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện id = " + eventId));

        if (event.getMatch() == null || !event.getMatch().getId().equals(matchId)) {
            throw new RuntimeException("Sự kiện id = " + eventId + " không thuộc trận đấu id = " + matchId);
        }

        playerStatsService.applyEvent(event, -1);
        matchEventRepository.delete(event);
        recalculateMatchScore(matchId);
        sendMatchEventRealtimeEvents(matchId);
    }

    private void applyRequest(MatchEvent event, MatchEventUpsertRequest request, Team team) {
        event.setMinute(request.getMinute());
        event.setExtraMinute(request.getExtraMinute());
        event.setEventOrder(request.getEventOrder());

        event.setEventType(request.getEventType());
        event.setGoalType(request.getGoalType());

        event.setTeam(team);
        event.setPlayer(findPlayerOrNull(request.getPlayerId()));
        event.setPlayerIn(findPlayerOrNull(request.getPlayerInId()));
        event.setAssistPlayer(findPlayerOrNull(request.getAssistPlayerId()));

        event.setNote(request.getNote());
    }

    private Player findPlayerOrNull(Long playerId) {
        if (playerId == null) {
            return null;
        }

        return playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cầu thủ id = " + playerId));
    }

    private void validateTeamBelongsToMatch(Match match, Team team) {
        Long homeTeamId = match.getHomeTeam().getTeam().getId();
        Long awayTeamId = match.getAwayTeam().getTeam().getId();

        if (!team.getId().equals(homeTeamId) && !team.getId().equals(awayTeamId)) {
            throw new RuntimeException("Đội " + team.getName() + " không thuộc trận đấu này");
        }
    }

    private void validateEventRequest(MatchEventUpsertRequest request) {
        if (request.getMinute() == null || request.getMinute() < 0 || request.getMinute() > 130) {
            throw new RuntimeException("Phút thi đấu không hợp lệ");
        }

        if (request.getExtraMinute() != null && request.getExtraMinute() < 0) {
            throw new RuntimeException("Phút bù giờ không hợp lệ");
        }

        if (request.getEventType() == null) {
            throw new RuntimeException("Loại sự kiện không được để trống");
        }

        if (request.getTeamId() == null) {
            throw new RuntimeException("Đội bóng không được để trống");
        }

        if (request.getEventType() == EventType.GOAL) {
            if (request.getPlayerId() == null) {
                throw new RuntimeException("Sự kiện bàn thắng cần có cầu thủ ghi bàn");
            }

            if (request.getGoalType() == null) {
                request.setGoalType(GoalType.NORMAL);
            }
        } else {
            request.setGoalType(null);
        }

        if (request.getEventType() == EventType.YELLOW_CARD || request.getEventType() == EventType.RED_CARD) {
            if (request.getPlayerId() == null) {
                throw new RuntimeException("Sự kiện thẻ cần có cầu thủ nhận thẻ");
            }
        }

        if (request.getEventType() == EventType.SUBSTITUTION) {
            if (request.getPlayerId() == null || request.getPlayerInId() == null) {
                throw new RuntimeException("Sự kiện thay người cần có cầu thủ rời sân và cầu thủ vào sân");
            }
        }
    }

    private MatchEventResponse toResponse(MatchEvent event) {
        Team team = event.getTeam();
        Player player = event.getPlayer();
        Player playerIn = event.getPlayerIn();
        Player assistPlayer = event.getAssistPlayer();

        return new MatchEventResponse(
                event.getId(),
                event.getMatch() != null ? event.getMatch().getId() : null,

                event.getMinute(),
                event.getExtraMinute(),
                event.getEventOrder(),

                event.getEventType(),
                event.getGoalType(),

                team != null ? team.getId() : null,
                team != null ? team.getName() : null,
                team != null ? team.getLogo() : null,

                player != null ? player.getId() : null,
                player != null ? player.getName() : null,

                playerIn != null ? playerIn.getId() : null,
                playerIn != null ? playerIn.getName() : null,

                assistPlayer != null ? assistPlayer.getId() : null,
                assistPlayer != null ? assistPlayer.getName() : null,

                event.getNote()
        );
    }
    @Transactional
    public void recalculateMatchScore(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu id = " + matchId));

        Long homeTeamId = match.getHomeTeam().getTeam().getId();
        Long awayTeamId = match.getAwayTeam().getTeam().getId();

        List<MatchEvent> events =
                matchEventRepository.findByMatchIdOrderByMinuteAscExtraMinuteAscEventOrderAscIdAsc(matchId);

        int homeScore = 0;
        int awayScore = 0;

        for (MatchEvent event : events) {
            if (event.getEventType() != EventType.GOAL || event.getTeam() == null) {
                continue;
            }

            Long eventTeamId = event.getTeam().getId();
            boolean isOwnGoal = event.getGoalType() == GoalType.OWN_GOAL;

            if (!isOwnGoal) {
                if (eventTeamId.equals(homeTeamId)) {
                    homeScore++;
                } else if (eventTeamId.equals(awayTeamId)) {
                    awayScore++;
                }
            } else {
                if (eventTeamId.equals(homeTeamId)) {
                    awayScore++;
                } else if (eventTeamId.equals(awayTeamId)) {
                    homeScore++;
                }
            }
        }

        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);

        matchRepository.save(match);
    }

    private void sendMatchEventRealtimeEvents(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y tráº­n Ä‘áº¥u id = " + matchId));

        Set<Long> userIds = findRelatedUserIds(match);

        realtimeEventService.sendToUsers(
                userIds,
                realtimeEvent("MATCH_EVENT_CHANGED", matchId, "MATCH_EVENT", "REFETCH_MATCH_EVENTS")
        );
        realtimeEventService.sendToUsers(
                userIds,
                realtimeEvent("MATCH_SCORE_UPDATED", matchId, "MATCH", "REFETCH_MATCH_DETAIL")
        );

        if (match.getStatus() == MatchStatus.FINISHED && match.getSeason() != null) {
            Long seasonId = match.getSeason().getId();
            standingService.recalculateBySeason(seasonId);
            realtimeEventService.sendToUsers(
                    userIds,
                    realtimeEvent("STANDING_UPDATED", seasonId, "STANDING", "REFETCH_STANDINGS")
            );
        }
    }

    private Set<Long> findRelatedUserIds(Match match) {
        Set<Long> userIds = new LinkedHashSet<>();

        userRepository.findUsersByRoleName("ROLE_ADMIN")
                .stream()
                .map(User::getId)
                .forEach(userIds::add);

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

        Optional<User> managerOpt =
                userRepository.findClubManagerByTeamIdAndRoleName(
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

    private void validatePlayerBelongsToTeam(Player player, Team team) {
        if (player == null) {
            return;
        }

        if (player.getTeam() == null || !player.getTeam().getId().equals(team.getId())) {
            throw new RuntimeException(
                    "Cầu thủ " + player.getName() + " không thuộc đội " + team.getName()
            );
        }
    }
}
