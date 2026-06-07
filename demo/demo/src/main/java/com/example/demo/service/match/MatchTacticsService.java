package com.example.demo.service.match;

import com.example.demo.dao.match.MatchLineupRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.match.MatchTacticsRepository;
import com.example.demo.dao.team.player.PlayerRepository;
import com.example.demo.dao.team.player.PlayerSeasonRepository;
import com.example.demo.dao.team.player.PlayerSuspensionRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.dto.lineups.*;
import com.example.demo.entity.*;
import com.example.demo.entity.match.Match;
import com.example.demo.entity.match.MatchLineup;
import com.example.demo.entity.match.MatchTactics;
import com.example.demo.entity.player.Player;
import com.example.demo.entity.season.PlayerSeason;
import com.example.demo.entity.season.Season;
import com.example.demo.entity.team.Team;
import com.example.demo.entity.user.User;
import com.example.demo.service.realtime.NotificationService;
import com.example.demo.service.team.player.PlayerStatsService;
import com.example.demo.service.realtime.RealtimeEventService;
import com.example.demo.service.season.SeasonTeamService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchTacticsService {

    private final MatchTacticsRepository matchTacticsRepository;
    private final MatchLineupRepository matchLineupRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerStatsService playerStatsService;
    private final PlayerSeasonRepository playerSeasonRepository;
    private final NotificationService notificationService;
    private final RealtimeEventService realtimeEventService;
    private final UserRepository userRepository;
    private final PlayerSuspensionRepository playerSuspensionRepository;
    private final SeasonTeamService seasonTeamService;


//  ==================== QUERY METHODS ====================

    // Lấy đội theo id
    public List<MatchTacticsResponse> getByMatch(Long matchId) {
        return matchTacticsRepository.findByMatchIdWithLineups(matchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Lấy lineup chi tiết của team theo trận đấu
    public MatchTacticsResponse getTeamLineup(Long matchId, Long teamId) {
        return matchTacticsRepository.findByMatchIdAndTeamId(matchId, teamId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException(
                        "Chưa có đội hình cho teamId = " + teamId + " trong matchId = " + matchId
                ));
    }


    //    Tìm trận đấy theo id
    private Match getMatchOrThrow(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu id = " + matchId));
    }

    //    Tìm đội theo id
    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng id = " + teamId));
    }

    // Tìm cầu thủ theo id
    private Player getPlayerOrThrow(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cầu thủ id = " + playerId));
    }
    // Lấy Lineup của trận đấu ( cả 2 team)
    public MatchLineupsResponse getLineupsByMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu id = " + matchId));

        List<MatchTacticsResponse> tacticsList = getByMatch(matchId);

        Long homeTeamId = match.getHomeTeam().getTeam().getId();
        Long awayTeamId = match.getAwayTeam().getTeam().getId();

        MatchTacticsResponse home = tacticsList.stream()
                .filter(t -> homeTeamId.equals(t.getTeamId()))
                .findFirst()
                .orElse(null);

        MatchTacticsResponse away = tacticsList.stream()
                .filter(t -> awayTeamId.equals(t.getTeamId()))
                .findFirst()
                .orElse(null);

        return new MatchLineupsResponse(
                match.getId(),
                home,
                away
        );
    }


// ==================== COMMAND METHODS ====================

    // Thêm hoặc cập nhật thông kê trận đấu
    private MatchTactics getOrCreateTactics(Match match, Team team) {
        return matchTacticsRepository
                .findByMatchIdAndTeamId(match.getId(), team.getId())
                .orElseGet(() -> {
                    MatchTactics newTactics = new MatchTactics();
                    newTactics.setMatch(match);
                    newTactics.setTeam(team);
                    return newTactics;
                });
    }


    private MatchTacticsResponse getTacticsResponse(Long tacticsId) {
        MatchTactics tactics = matchTacticsRepository.findWithLineupsById(tacticsId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tactics sau khi lưu"));

        return toResponse(tactics);
    }
    // Xóa thống kê trận đấu
    @Transactional
    public void deleteTactics(Long matchId, Long tacticsId) {
        MatchTactics tactics = matchTacticsRepository.findById(tacticsId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tactics id = " + tacticsId));

        if (tactics.getMatch() == null || !tactics.getMatch().getId().equals(matchId)) {
            throw new RuntimeException("Tactics id = " + tacticsId + " không thuộc trận đấu id = " + matchId);
        }

        if (tactics.getTeam() == null || tactics.getTeam().getId() == null) {
            throw new RuntimeException("Đội bóng không thuộc trận đấu này.");
        }

        validateActiveSeasonTeamForTactic(tactics.getMatch(), tactics.getTeam().getId());

        Long seasonId = tactics.getMatch().getSeason() != null ? tactics.getMatch().getSeason().getId() : null;

        matchTacticsRepository.delete(tactics);

        if (seasonId != null) {
            playerStatsService.recalculateBySeason(seasonId);
        }

        sendLineupDeletedEventToAdmins(matchId);
    }

    @Transactional
    public void deleteTeamLineup(Long matchId, Long teamId) {
        MatchTactics tactics = matchTacticsRepository.findByMatchIdAndTeamId(matchId, teamId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy đội hình của teamId = " + teamId + " trong matchId = " + matchId
                ));

        Match match = tactics.getMatch() != null ? tactics.getMatch() : getMatchOrThrow(matchId);
        validateActiveSeasonTeamForLineup(match, teamId);

        Long seasonId = tactics.getMatch() != null && tactics.getMatch().getSeason() != null
                ? tactics.getMatch().getSeason().getId()
                : null;

        matchTacticsRepository.delete(tactics);

        if (seasonId != null) {
            playerStatsService.recalculateBySeason(seasonId);
        }

        sendLineupDeletedEventToAdmins(matchId);
    }

    private SystemRule getRequiredRule(Season season) {
        if (season == null) {
            throw new RuntimeException("Không tìm thấy mùa giải của trận đấu");
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
// ==================== BUSINESS HELPERS ====================

    //    Cập nhật lineup mới
    private void replaceLineups(
            MatchTactics tactics,
            List<MatchLineupRequest> lineupRequests,
            Team team,
            Match match
    ) {
        matchLineupRepository.deleteByMatchTacticsId(tactics.getId());
        matchLineupRepository.flush();

        for (MatchLineupRequest lineupRequest : lineupRequests) {
            MatchLineup lineup = buildLineup(tactics, lineupRequest, team, match);
            matchLineupRepository.save(lineup);
        }
    }

//  Xây dựng lineup (gắn PlayerSeason để biết cầu thủ thuộc đội nào trong mùa giải)
    private MatchLineup buildLineup(
            MatchTactics tactics,
            MatchLineupRequest lineupRequest,
            Team team,
            Match match
    ) {
        Player player = getPlayerOrThrow(lineupRequest.getPlayerId());

        PlayerSeason playerSeason = playerSeasonRepository
                .findByPlayerIdAndTeamIdAndSeasonId(
                        player.getId(),
                        team.getId(),
                        match.getSeason().getId()
                )
                .orElseThrow(() -> new RuntimeException("Không tìm thấy PlayerSeason hợp lệ"));

        MatchLineup lineup = new MatchLineup();
        lineup.setMatchTactics(tactics);
        lineup.setPlayer(player);
        lineup.setPosition(lineupRequest.getPosition());
        lineup.setShirtNumber(lineupRequest.getShirtNumber());
        lineup.setIsStarting(Boolean.TRUE.equals(lineupRequest.getIsStarting()));
        lineup.setLineupOrder(lineupRequest.getLineupOrder());
        lineup.setRole(lineupRequest.getRole());
        lineup.setPlayerSeason(playerSeason);

        return lineup;
    }

    private void recalculatePlayerStats(Match match) {
        if (match.getSeason() != null) {
            playerStatsService.recalculateBySeason(match.getSeason().getId());
        }
    }

//    Xử lý nghiệp vụ cập nhật , hoặc thêm đội hình đội hình
        @Transactional
        public MatchTacticsResponse upsertTeamLineup(
        Long matchId,
        Long teamId,
        MatchTacticsUpsertRequest request
)
        {
         validateRequestNotNull(request);

         Match match = getMatchOrThrow(matchId);
         Team team = getTeamOrThrow(teamId);
         validateActiveSeasonTeamForLineup(match, teamId);
         SystemRule rule = getRequiredRule(match.getSeason());

         validateLineupRequest(match, team, request, rule);

         MatchTactics tactics = getOrCreateTactics(match, team);
         boolean existedBefore = tactics.getId() != null;

         applyTacticsRequest(tactics, request);

         MatchTactics savedTactics = matchTacticsRepository.save(tactics);

         replaceLineups(savedTactics, request.getLineups(), team, match);

         recalculatePlayerStats(match);


         notifyAdminsAboutLineup(matchId, teamId, existedBefore);

         return getTacticsResponse(savedTactics.getId());
}

//    Xây dựng tên trậm đấu
    private String buildMatchName(Match match) {
        String homeName = match.getHomeTeam() != null
                && match.getHomeTeam().getTeam() != null
                ? match.getHomeTeam().getTeam().getName()
                : "Đội chủ nhà";

        String awayName = match.getAwayTeam() != null
                && match.getAwayTeam().getTeam() != null
                ? match.getAwayTeam().getTeam().getName()
                : "Đội khách";

        return homeName + " vs " + awayName;
    }



    //    Kiểm tra cầu thủ nước ngoài
    private boolean isForeignPlayer(Player player) {
        if (player == null || player.getNationality() == null) {
            return false;
        }

        String nationality = player.getNationality().trim().toLowerCase();

        return !nationality.equals("việt nam")
                && !nationality.equals("viet nam")
                && !nationality.equals("vietnam")
                && !nationality.equals("vn");
    }
// ==================== MAPPING HELPERS ====================
    private void applyTacticsRequest(MatchTactics tactics, MatchTacticsUpsertRequest request) {
    tactics.setFormationName(request.getFormationName());
    tactics.setDescription(request.getDescription());
    }

    private MatchTacticsResponse toResponse(MatchTactics tactics) {
        Team team = tactics.getTeam();


        List<MatchLineupResponse> lineupResponses = tactics.getLineups() == null
                ? List.of()
                : tactics.getLineups()
                .stream()
                .sorted(
                        Comparator
                                .comparing(
                                        MatchLineup::getIsStarting,
                                        Comparator.nullsLast(Boolean::compareTo)
                                )
                                .reversed()
                                .thenComparing(
                                        MatchLineup::getLineupOrder,
                                        Comparator.nullsLast(Integer::compareTo)
                                )
                                .thenComparing(
                                        MatchLineup::getShirtNumber,
                                        Comparator.nullsLast(Integer::compareTo)
                                )
                )
                .map(this::toLineupResponse)
                .toList();

        return new MatchTacticsResponse(
                tactics.getId(),
                tactics.getMatch() != null ? tactics.getMatch().getId() : null,
                team != null ? team.getId() : null,
                team != null ? team.getName() : null,
                team != null ? team.getLogo() : null,
                tactics.getFormationName(),
                tactics.getDescription(),
                lineupResponses
        );
    }



    private MatchLineupResponse toLineupResponse(MatchLineup lineup) {
        Player player = lineup.getPlayer();

        return new MatchLineupResponse(
                lineup.getId(),
                player != null ? player.getId() : null,
                player != null ? player.getName() : null,
                player != null ? player.getAvatar() : null,
                toPositionCode(lineup.getPosition()),
                lineup.getShirtNumber(),
                lineup.getIsStarting(),
                lineup.getLineupOrder(),
                lineup.getRole()
        );
    }

    private String toPositionCode(String position) {
        if (position == null || position.isBlank()) {
            return null;
        }

        String normalized = position.trim().toLowerCase();

        return switch (normalized) {
            case "thủ môn", "thu mon", "gk", "goalkeeper" -> "GK";
            case "hậu vệ", "hau ve", "df", "defender" -> "DF";
            case "tiền vệ", "tien ve", "mf", "midfielder" -> "MF";
            case "tiền đạo", "tien dao", "fw", "forward", "striker" -> "FW";
            default -> position;
        };
    }
//    =================== VALIDATE HELPERS=======================

//    Hàm tổng hợp validate
    private void validateActiveSeasonTeamForLineup(Match match, Long teamId) {
        validateActiveSeasonTeamForWrite(
                match,
                teamId,
                "Đội bóng đã bị vô hiệu hóa trong mùa giải này, không thể cập nhật."
        );
    }

    private void validateActiveSeasonTeamForTactic(Match match, Long teamId) {
        validateActiveSeasonTeamForWrite(
                match,
                teamId,
                "Đội bóng đã bị vô hiệu hóa trong mùa giải này, không thể cập nhật chiến thuật."
        );
    }

    private void validateActiveSeasonTeamForWrite(Match match, Long teamId, String inactiveMessage) {
        if (match.getSeason() == null || match.getSeason().getId() == null) {
            throw new RuntimeException("Trận đấu chưa có thông tin mùa giải hợp lệ.");
        }

        if (!isTeamInMatch(match, teamId)) {
            throw new RuntimeException("Đội bóng không thuộc trận đấu này.");
        }

        try {
            seasonTeamService.getActiveSeasonTeamOrThrow(match.getSeason().getId(), teamId);
        } catch (RuntimeException ex) {
            if (isInactiveSeasonTeamError(ex)) {
                throw new RuntimeException(inactiveMessage);
            }
            throw ex;
        }
    }

    private boolean isTeamInMatch(Match match, Long teamId) {
        if (teamId == null) {
            return false;
        }

        Long homeTeamId = match.getHomeTeam() != null && match.getHomeTeam().getTeam() != null
                ? match.getHomeTeam().getTeam().getId()
                : null;
        Long awayTeamId = match.getAwayTeam() != null && match.getAwayTeam().getTeam() != null
                ? match.getAwayTeam().getTeam().getId()
                : null;

        return teamId.equals(homeTeamId) || teamId.equals(awayTeamId);
    }

    private boolean isInactiveSeasonTeamError(RuntimeException ex) {
        String message = ex.getMessage();
        return message != null
                && (message.contains("vô hiệu")
                || message.contains("vÃ´")
                || message.contains("hiệu hóa")
                || message.contains("hiá»‡u hÃ³a"));
    }

    private void validateLineupRequest(
            Match match,
            Team team,
            MatchTacticsUpsertRequest request,
            SystemRule rule
    ) {
        validateTeamBelongsToMatch(match, team);

        List<MatchLineupRequest> lineups = request.getLineups();

        validateNoDuplicatePlayers(lineups);
        validateStartingPlayers(lineups);
        validateForeignPlayersOnField(lineups, team, match.getSeason(), rule);
        validateLineupSizeByRule(lineups, rule);
        validateFormation(request.getFormationName(), lineups);
        validateMatchdaySquadSize(lineups);
        validateLineupPlayerEligibility(lineups, team, match);
    }

//    Kiểm tra cầu thủ
    private void validateLineupPlayerEligibility(
            List<MatchLineupRequest> lineups,
            Team team,
            Match match
    ) {
        for (MatchLineupRequest lineupRequest : lineups) {
            Player player = getPlayerOrThrow(lineupRequest.getPlayerId());

            validatePlayerBelongsToTeamInSeason(player, team, match.getSeason());
            validatePlayerNotSuspended(player, match);
        }
    }

    //    Kiểm tra dữ liệu cập nhật đội hình
    private void validateRequestNotNull(MatchTacticsUpsertRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu đội hình không được để trống");
        }

        if (request.getLineups() == null || request.getLineups().isEmpty()) {
            throw new RuntimeException("Danh sách cầu thủ trong đội hình không được để trống");
        }
    }

    private void validateMatchdaySquadSize(List<MatchLineupRequest> lineups) {
        if (lineups == null || lineups.isEmpty()) {
            throw new RuntimeException("Danh sách đội hình không được để trống");
        }

        if (lineups.size() != 16) {
            throw new RuntimeException("Danh sách đăng ký trận đấu phải có đúng 16 cầu thủ");
        }

        long startingCount = lineups.stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsStarting()))
                .count();

        long substituteCount = lineups.size() - startingCount;

        if (startingCount != 11 || substituteCount != 5) {
            throw new RuntimeException("Đội hình phải gồm 11 cầu thủ đá chính và 5 cầu thủ dự bị");
        }
    }



    private void validateStartingPlayers(List<MatchLineupRequest> lineups) {
        if (lineups == null || lineups.isEmpty()) {
            throw new RuntimeException("Danh sách đội hình không được để trống");
        }

        long startingCount = lineups.stream()
                .filter(lineup -> Boolean.TRUE.equals(lineup.getIsStarting()))
                .count();

        if (startingCount != 11) {
            throw new RuntimeException("Đội hình ra sân phải có đúng 11 cầu thủ đá chính");
        }

        long goalkeeperCount = lineups.stream()
                .filter(lineup -> Boolean.TRUE.equals(lineup.getIsStarting()))
                .filter(lineup -> "GK".equalsIgnoreCase(lineup.getPosition()))
                .count();

        if (goalkeeperCount != 1) {
            throw new RuntimeException("Đội hình đá chính phải có đúng 1 thủ môn");
        }
    }
    private void validateTeamBelongsToMatch(Match match, Team team) {
        Long homeTeamId = match.getHomeTeam().getTeam().getId();
        Long awayTeamId = match.getAwayTeam().getTeam().getId();

        if (!team.getId().equals(homeTeamId) && !team.getId().equals(awayTeamId)) {
            throw new RuntimeException("Đội " + team.getName() + " không thuộc trận đấu này");
        }
    }

    private void validateNoDuplicatePlayers(List<MatchLineupRequest> lineups) {
        if (lineups == null || lineups.isEmpty()) {
            throw new RuntimeException("Danh sách đội hình không được để trống");
        }

        Set<Long> playerIds = new HashSet<>();

        for (MatchLineupRequest lineup : lineups) {
            if (lineup.getPlayerId() == null) {
                throw new RuntimeException("Cầu thủ trong đội hình không được để trống");
            }

            if (lineup.getPosition() == null || lineup.getPosition().isBlank()) {
                throw new RuntimeException("Vị trí thi đấu của cầu thủ không được để trống");
            }

            if (!playerIds.add(lineup.getPlayerId())) {
                throw new RuntimeException("Cầu thủ id = " + lineup.getPlayerId() + " bị trùng trong đội hình");
            }
        }
    }
    private void validatePlayerBelongsToTeamInSeason(Player player, Team team, Season season) {
        if (season == null) {
            throw new RuntimeException("Trận đấu chưa có mùa giải");
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
    private void validateLineupSizeByRule(
            List<MatchLineupRequest> lineups,
            SystemRule rule
    ) {
        if (lineups == null || lineups.isEmpty()) {
            throw new RuntimeException("Danh sách đội hình không được để trống");
        }

        if (rule.getMaxPlayers() != null && lineups.size() > rule.getMaxPlayers()) {
            throw new RuntimeException(
                    "Danh sách cầu thủ vượt quá giới hạn của bộ luật. Tối đa: "
                            + rule.getMaxPlayers()
            );
        }

        if (rule.getMinPlayers() != null && lineups.size() < rule.getMinPlayers()) {
            throw new RuntimeException(
                    "Danh sách cầu thủ chưa đạt tối thiểu theo bộ luật. Tối thiểu: "
                            + rule.getMinPlayers()
            );
        }
    }
    private void validateFormation(String formationName, List<MatchLineupRequest> lineups) {
        if (formationName == null || formationName.isBlank()) {
            throw new RuntimeException("Sơ đồ chiến thuật không được để trống");
        }

        String[] parts = formationName.trim().split("-");

        int formationTotal = 0;

        try {
            for (String part : parts) {
                int value = Integer.parseInt(part.trim());

                if (value <= 0) {
                    throw new RuntimeException("Sơ đồ chiến thuật không hợp lệ");
                }

                formationTotal += value;
            }
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Sơ đồ chiến thuật không đúng định dạng. Ví dụ hợp lệ: 4-3-3, 5-3-2, 3-4-3");
        }

        if (formationTotal != 10) {
            throw new RuntimeException("Sơ đồ chiến thuật phải có tổng 10 cầu thủ ngoài sân");
        }

        long startingCount = lineups.stream()
                .filter(lineup -> Boolean.TRUE.equals(lineup.getIsStarting()))
                .count();

        if (startingCount != 11) {
            throw new RuntimeException("Đội hình đá chính phải có đúng 11 cầu thủ");
        }

        long goalkeeperCount = lineups.stream()
                .filter(lineup -> Boolean.TRUE.equals(lineup.getIsStarting()))
                .filter(lineup -> "GK".equalsIgnoreCase(lineup.getPosition()))
                .count();

        if (goalkeeperCount != 1) {
            throw new RuntimeException("Đội hình đá chính phải có đúng 1 thủ môn");
        }
    }

    private void validatePlayerNotSuspended(Player player, Match match) {
        boolean suspended = playerSuspensionRepository
                .existsByPlayerIdAndSuspendedMatchIdAndServedFalse(player.getId(), match.getId());

        if (suspended) {
            throw new RuntimeException("Cầu thủ " + player.getName() + " đang bị treo giò trận này");
        }
    }

    private void validateForeignPlayersOnField(
            List<MatchLineupRequest> lineups,
            Team team,
            Season season,
            SystemRule rule
    ) {
        int maxForeignOnField = rule.getMaxForeignPlayersOnField() != null
                ? rule.getMaxForeignPlayersOnField()
                : 3;

        long foreignStartingCount = 0;

        for (MatchLineupRequest lineup : lineups) {
            if (!Boolean.TRUE.equals(lineup.getIsStarting())) {
                continue;
            }

            Player player = playerRepository.findById(lineup.getPlayerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cầu thủ id = " + lineup.getPlayerId()));

            validatePlayerBelongsToTeamInSeason(player, team, season);

            if (isForeignPlayer(player)) {
                foreignStartingCount++;
            }
        }

        if (foreignStartingCount > maxForeignOnField) {
            throw new RuntimeException(
                    "Số cầu thủ ngoại trong đội hình đá chính vượt quá giới hạn: "
                            + maxForeignOnField
            );
        }
    }

    // ==================== REALTIME HELPERS ====================
    private void notifyAdminsAboutLineup(Long matchId, Long teamId, boolean existedBefore) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng"));

        String matchName = buildMatchName(match);

        List<User> admins = userRepository.findUsersByRoleName("ROLE_ADMIN");
        RealtimeEventDTO event = realtimeEvent(
                existedBefore ? "LINEUP_UPDATED" : "LINEUP_SUBMITTED",
                matchId,
                "MATCH_LINEUP",
                "REFETCH_LINEUPS"
        );

        for (User admin : admins) {
            if (existedBefore) {
                notificationService.notifyLineupUpdatedToAdmin(
                        admin.getId(),
                        team.getName(),
                        matchName,
                        matchId
                );
            } else {
                notificationService.notifyLineupSubmittedToAdmin(
                        admin.getId(),
                        team.getName(),
                        matchName,
                        matchId
                );
            }

            realtimeEventService.sendToUser(admin.getId(), event);
        }

        realtimeEventService.sendToPublicMatch(matchId, event);
    }








    private void sendLineupDeletedEventToAdmins(Long matchId) {
        List<User> admins = userRepository.findUsersByRoleName("ROLE_ADMIN");
        RealtimeEventDTO event = realtimeEvent(
                "LINEUP_DELETED",
                matchId,
                "MATCH_LINEUP",
                "REFETCH_LINEUPS"
        );

        for (User admin : admins) {
            realtimeEventService.sendToUser(admin.getId(), event);
        }

        realtimeEventService.sendToPublicMatch(matchId, event);
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
