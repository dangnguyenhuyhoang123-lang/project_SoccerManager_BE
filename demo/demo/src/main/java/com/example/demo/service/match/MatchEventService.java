package com.example.demo.service.match;

import com.example.demo.dao.match.MatchEventRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.team.player.PlayerRepository;
import com.example.demo.dao.team.player.PlayerSeasonRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.dto.matchevent.MatchEventResponse;
import com.example.demo.dto.matchevent.MatchEventUpsertRequest;
import com.example.demo.entity.*;
import com.example.demo.entity.match.*;
import com.example.demo.entity.player.Player;
import com.example.demo.entity.season.Season;
import com.example.demo.entity.season.SeasonTeam;
import com.example.demo.entity.team.Team;
import com.example.demo.entity.user.User;
import com.example.demo.service.team.player.PlayerStatsService;
import com.example.demo.service.team.player.PlayerSuspensionService;
import com.example.demo.service.realtime.RealtimeEventService;
import com.example.demo.service.season.StandingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

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
    private final PlayerSuspensionService playerSuspensionService;




//============================ QUERY METHODS ====================

    //    Lấy toàn bộ sự kiện của 1 trận đấu
    public List<MatchEventResponse> getEventsByMatch(Long matchId) {
        return matchEventRepository.findByMatchIdOrderByMinuteAscExtraMinuteAscEventOrderAscIdAsc(matchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Lấy cầu thủ theo id
    private Player findPlayerOrNull(Long playerId) {
        if (playerId == null) {
            return null;
        }

        return playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cầu thủ id = " + playerId));
    }

    /**
     * Lấy trận đấu theo id.
     * Nếu không tồn tại thì dừng thao tác tạo/cập nhật/xóa sự kiện.
     */
    private Match getMatchOrThrow(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu id = " + matchId));
    }

    /**
     * Lấy đội bóng tạo sự kiện.
     * Team này phải là đội nhà hoặc đội khách của trận đấu.
     */
    private Team getTeamOrThrow(Long teamId) {
        if (teamId == null) {
            throw new RuntimeException("Đội bóng tạo sự kiện không được để trống");
        }

        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng id = " + teamId));
    }

    /**
     * Lấy sự kiện trận đấu theo id.
     * Dùng khi cập nhật hoặc xóa sự kiện.
     */
    private MatchEvent getMatchEventOrThrow(Long eventId) {
        return matchEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện id = " + eventId));
    }




//  Lấy bộ luật theo mùa giải tương ứng
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



//    ==================== COMMAND METHODS ====================

    //  Thêm sự kiện cho trận đấu
    /**
     * Tạo sự kiện mới cho trận đấu.
     * Sau khi lưu, hệ thống cập nhật thống kê cầu thủ, tính lại tỉ số và phát realtime.
     */
    @Transactional
    public MatchEventResponse createEvent(Long matchId, MatchEventUpsertRequest request) {
        Match match = getMatchOrThrow(matchId);
        Team team = getTeamOrThrow(request != null ? request.getTeamId() : null);

        validateEventRequest(match, request, team, null);

        MatchEvent event = new MatchEvent();
        event.setMatch(match);
        applyRequest(event, request, team);

        MatchEvent savedEvent = matchEventRepository.save(event);

        playerStatsService.applyEvent(savedEvent, 1);
        recalculateMatchScore(matchId);
        sendMatchEventRealtimeEvents(matchId);

        return toResponse(savedEvent);
    }

    // Cập nhật sự kiện đã có của trận đấu đó
    /**
     * Cập nhật sự kiện đã có của một trận đấu.
     * Trước khi cập nhật cần trừ thống kê từ event cũ, sau đó cộng lại thống kê từ event mới.
     */
    @Transactional
    public MatchEventResponse updateEvent(
            Long matchId,
            Long eventId,
            MatchEventUpsertRequest request
    ) {
        MatchEvent event = getMatchEventOrThrow(eventId);

        if (event.getMatch() == null || !event.getMatch().getId().equals(matchId)) {
            throw new RuntimeException("Sự kiện id = " + eventId + " không thuộc trận đấu id = " + matchId);
        }

        Match match = getMatchOrThrow(matchId);
        Team team = getTeamOrThrow(request != null ? request.getTeamId() : null);

        validateEventRequest(match, request, team, eventId);

        // Trừ thống kê từ event cũ trước khi thay đổi dữ liệu.
        playerStatsService.applyEvent(event, -1);

        applyRequest(event, request, team);

        MatchEvent savedEvent = matchEventRepository.save(event);

        // Cộng lại thống kê theo dữ liệu event mới.
        playerStatsService.applyEvent(savedEvent, 1);

        recalculateMatchScore(matchId);
        sendMatchEventRealtimeEvents(matchId);

        return toResponse(savedEvent);
    }


    /**
     * Xóa sự kiện của một trận đấu.
     * Sau khi xóa, hệ thống trừ thống kê cầu thủ, tính lại tỉ số và phát realtime.
     */
    @Transactional
    public void deleteEvent(Long matchId, Long eventId) {
        MatchEvent event = getMatchEventOrThrow(eventId);

        if (event.getMatch() == null || !event.getMatch().getId().equals(matchId)) {
            throw new RuntimeException("Sự kiện id = " + eventId + " không thuộc trận đấu id = " + matchId);
        }

        playerStatsService.applyEvent(event, -1);
        matchEventRepository.delete(event);

        recalculateMatchScore(matchId);
        sendMatchEventRealtimeEvents(matchId);
    }






//  ==================== BUSINESS HELPERS ====================
      /*Xử lý lưu trữ , cập nhật sự kiện*/
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

    //    Cập nhật kết quả trận đấu khi hoàn tất thêm sự kiện là bàn thắng
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



//  =================== VALIDATE HELPERS=======================



    /**
     * Kiểm tra request tạo/cập nhật sự kiện trận đấu.

     * Các ràng buộc được kiểm tra theo rule của mùa giải tương ứng:
     * - GOAL: cần cầu thủ ghi bàn, loại bàn thắng hợp lệ theo allowedGoalTypes.
     * - CARD: cần cầu thủ nhận thẻ.
     * - SUBSTITUTION: cần cầu thủ ra sân, cầu thủ vào sân và không vượt quá số lượt thay người.
     */
    private void validateEventRequest(
            Match match,
            MatchEventUpsertRequest request,
            Team team,
            Long currentEventId
    ) {
        validateCommonEventFields(match, request, team);

        SystemRule rule = getRequiredRule(match.getSeason());

        validateEventMinute(request, rule);

        switch (request.getEventType()) {
            case GOAL -> validateGoalEvent(match, request, team, rule);
            case RED_CARD,YELLOW_CARD -> validateCardEvent(match, request, team);
            case SUBSTITUTION -> validateSubstitutionEvent(match, request, team, rule, currentEventId);
            default -> throw new RuntimeException("Loại sự kiện trận đấu không hợp lệ");
        }
    }


    /**
     * Kiểm tra các dữ liệu bắt buộc dùng chung cho mọi loại sự kiện trận đấu.
     */
    private void validateCommonEventFields(
            Match match,
            MatchEventUpsertRequest request,
            Team team
    ) {
        if (match == null) {
            throw new RuntimeException("Trận đấu không được để trống");
        }

        if (request == null) {
            throw new RuntimeException("Dữ liệu sự kiện không được để trống");
        }

        if (request.getEventType() == null) {
            throw new RuntimeException("Loại sự kiện không được để trống");
        }

        validateTeamBelongsToMatch(match, team);

    }

    /**
     * Kiểm tra phút xảy ra sự kiện.
     * Nếu rule có cấu hình maxGoalMinute thì không cho nhập sự kiện vượt quá phút tối đa.
     */
    private void validateEventMinute(
            MatchEventUpsertRequest request,
            SystemRule rule
    ) {
        if (request.getMinute() == null) {
            throw new RuntimeException("Phút xảy ra sự kiện không được để trống");
        }

        if (request.getMinute() < 0) {
            throw new RuntimeException("Phút xảy ra sự kiện không được âm");
        }

        Integer extraMinute = request.getExtraMinute() != null
                ? request.getExtraMinute()
                : 0;

        if (extraMinute < 0) {
            throw new RuntimeException("Phút bù giờ không được âm");
        }

        if (rule.getMaxGoalMinute() != null && request.getMinute() > rule.getMaxGoalMinute()) {
            throw new RuntimeException(
                    "Phút sự kiện không được vượt quá " + rule.getMaxGoalMinute()
            );
        }
    }


    /**
     * Kiểm tra sự kiện bàn thắng.
     *
     * Quy ước:
     * - playerId là cầu thủ ghi bàn.
     * - assistPlayerId là cầu thủ kiến tạo, có thể null.
     * - goalType phải hợp lệ theo rule mùa giải.
     */
    private void validateGoalEvent(
            Match match,
            MatchEventUpsertRequest request,
            Team team,
            SystemRule rule
    ) {
        if (request.getPlayerId() == null) {
            throw new RuntimeException("Sự kiện bàn thắng phải có cầu thủ ghi bàn");
        }

        if (request.getGoalType() == null) {
            throw new RuntimeException("Sự kiện bàn thắng phải có loại bàn thắng");
        }

        validateGoalTypeAllowed(request, rule);
        validatePlayerCanPlayForTeamInMatch(request.getPlayerId(), team, match);

        if (request.getAssistPlayerId() != null) {
            validatePlayerCanPlayForTeamInMatch(request.getAssistPlayerId(), team, match);
        }
    }

    /**
     * Kiểm tra sự kiện thẻ phạt.
     *
     * Quy ước:
     * - playerId là cầu thủ nhận thẻ.
     */
    private void validateCardEvent(
            Match match,
            MatchEventUpsertRequest request,
            Team team
    ) {
        if (request.getPlayerId() == null) {
            throw new RuntimeException("Sự kiện thẻ phạt phải có cầu thủ nhận thẻ");
        }

        validatePlayerCanPlayForTeamInMatch(request.getPlayerId(), team, match);
    }


    /**
     * Kiểm tra sự kiện thay người.
     *
     * Quy ước:
     * - playerId là cầu thủ ra sân.
     * - playerInId là cầu thủ vào sân.
     */
    /**
     * Kiểm tra sự kiện thay người.
     *
     * Quy ước:
     * - playerId là cầu thủ ra sân.
     * - playerInId là cầu thủ vào sân.
     * - Số lượt thay người không được vượt quá maxSubstitution của rule.
     */
    private void validateSubstitutionEvent(
            Match match,
            MatchEventUpsertRequest request,
            Team team,
            SystemRule rule,
            Long currentEventId
    ) {
        if (request.getPlayerId() == null) {
            throw new RuntimeException("Sự kiện thay người phải có cầu thủ ra sân");
        }

        if (request.getPlayerInId() == null) {
            throw new RuntimeException("Sự kiện thay người phải có cầu thủ vào sân");
        }

        if (request.getPlayerId().equals(request.getPlayerInId())) {
            throw new RuntimeException("Cầu thủ ra sân và vào sân không được trùng nhau");
        }

        validateSubstitutionLimit(
                match.getId(),
                team.getId(),
                rule,
                currentEventId
        );

        validatePlayerCanPlayForTeamInMatch(request.getPlayerId(), team, match);
        validatePlayerCanPlayForTeamInMatch(request.getPlayerInId(), team, match);
    }


    /**
     * Kiểm tra cầu thủ có thuộc CLB và mùa giải của trận đấu hay không.
     * Tránh trường hợp gán sự kiện cho cầu thủ không được đăng ký thi đấu trong mùa giải.
     */
    private void validatePlayerCanPlayForTeamInMatch(
            Long playerId,
            Team team,
            Match match
    ) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cầu thủ id = " + playerId));

        if (player.getTeam() == null || !player.getTeam().getId().equals(team.getId())) {
            throw new RuntimeException("Cầu thủ " + player.getName() + " không thuộc CLB " + team.getName());
        }

        if (match.getSeason() == null) {
            throw new RuntimeException("Trận đấu chưa thuộc mùa giải hợp lệ");
        }


        boolean existsInSeason = playerSeasonRepository
                .findByPlayerIdAndTeamIdAndSeasonId(player.getId(), team.getId(), match.getSeason().getId())
                .isPresent();

        if (!existsInSeason) {
            throw new RuntimeException(
                    "Cầu thủ " + player.getName() + " chưa được đăng ký cho mùa giải này"
            );
        }
    }

    /**
     * Kiểm tra đội tạo sự kiện có phải đội nhà hoặc đội khách của trận đấu hay không.
     */
    private void validateTeamBelongsToMatch(Match match, Team team) {
        Long teamId = team.getId();

        Long homeTeamId = match.getHomeTeam() != null
                && match.getHomeTeam().getTeam() != null
                ? match.getHomeTeam().getTeam().getId()
                : null;

        Long awayTeamId = match.getAwayTeam() != null
                && match.getAwayTeam().getTeam() != null
                ? match.getAwayTeam().getTeam().getId()
                : null;

        if (!teamId.equals(homeTeamId) && !teamId.equals(awayTeamId)) {
            throw new RuntimeException("Đội bóng không thuộc trận đấu này");
        }
    }


    /**
     * Kiểm tra loại bàn thắng có nằm trong danh sách allowedGoalTypes của rule hay không.
     */
    private void validateGoalTypeAllowed(
            MatchEventUpsertRequest request,
            SystemRule rule
    ) {
        String allowedGoalTypes = rule.getAllowedGoalTypes();

        if (allowedGoalTypes == null || allowedGoalTypes.isBlank()) {
            throw new RuntimeException("Bộ luật chưa cấu hình loại bàn thắng hợp lệ");
        }

        String requestedGoalType = request.getGoalType().name();

        boolean allowed = Arrays.stream(allowedGoalTypes.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .anyMatch(type -> type.equals(requestedGoalType));

        if (!allowed) {
            throw new RuntimeException("Loại bàn thắng không được phép trong mùa giải này");
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

//===================== MAPPING HELPERS =====================
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


//  =================== REALTIME HELPERS==================


    //    Tìm kiếm các user là quản lý câu lạc bộ liên quan
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

    //  Lấy tả tất cả user liên quan đến việc cập nhật trận đấu
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


    private void sendMatchEventRealtimeEvents(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tran dau id = " + matchId));

        Set<Long> userIds = findRelatedUserIds(match);

        RealtimeEventDTO matchEventChangedEvent = realtimeEvent(
                "MATCH_EVENT_CHANGED",
                matchId,
                "MATCH_EVENT",
                "REFETCH_MATCH_EVENTS"
        );

        RealtimeEventDTO matchScoreUpdatedEvent = realtimeEvent(
                "MATCH_SCORE_UPDATED",
                matchId,
                "MATCH",
                "REFETCH_MATCH_DETAIL"
        );

        // Gửi cho admin / club manager liên quan
        realtimeEventService.sendToUsers(userIds, matchEventChangedEvent);
        realtimeEventService.sendToUsers(userIds, matchScoreUpdatedEvent);

        // Gửi cho public MatchDetail
        realtimeEventService.sendToPublicMatch(matchId, matchEventChangedEvent);
        realtimeEventService.sendToPublicMatch(matchId, matchScoreUpdatedEvent);

        // Gửi cho public danh sách trận đấu
        realtimeEventService.sendToPublicMatches(matchScoreUpdatedEvent);

        if (match.getStatus() == MatchStatus.FINISHED && match.getSeason() != null) {
            Long seasonId = match.getSeason().getId();

            playerSuspensionService.generateSuspensionsAfterMatch(match.getId());
            standingService.recalculateBySeason(seasonId);

            RealtimeEventDTO standingUpdatedEvent = realtimeEvent(
                    "STANDING_UPDATED",
                    seasonId,
                    "STANDING",
                    "REFETCH_STANDINGS"
            );

            // Gửi cho admin / club manager liên quan
            realtimeEventService.sendToUsers(userIds, standingUpdatedEvent);

            // Gửi cho public bảng xếp hạng
            realtimeEventService.sendToPublicStandings(seasonId, standingUpdatedEvent);
        }
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
