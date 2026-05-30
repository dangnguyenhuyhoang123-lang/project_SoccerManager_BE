package com.example.demo.service;

import com.example.demo.dao.match.MatchLineupRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.match.MatchTacticsRepository;
import com.example.demo.dao.player.PlayerRepository;
import com.example.demo.dao.player.PlayerSeasonRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.dto.lineups.*;
import com.example.demo.entity.*;
import com.example.demo.entity.user.User;
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

//    public List<MatchTacticsResponse> getByMatch(Long matchId) {
//        return matchTacticsRepository.findByMatchId(matchId)
//                .stream()
//                .map(this::toResponse)
//                .toList();
//    }

    public List<MatchTacticsResponse> getByMatch(Long matchId) {
        return matchTacticsRepository.findByMatchIdWithLineups(matchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional
    public MatchTacticsResponse upsertTeamLineup(
            Long matchId,
            Long teamId,
            MatchTacticsUpsertRequest request
    ) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu id = " + matchId));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng id = " + teamId));

        validateTeamBelongsToMatch(match, team);
        SystemRule rule = getRequiredRule(match.getSeason());
        validateNoDuplicatePlayers(request.getLineups());

        validateStartingPlayers(request.getLineups());

        validateLineupSizeByRule(request.getLineups(), rule);

        validateFormation(request.getFormationName(), request.getLineups());

        for (MatchLineupRequest lineupRequest : request.getLineups()) {
            Player player = playerRepository.findById(lineupRequest.getPlayerId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy cầu thủ id = " + lineupRequest.getPlayerId()
                    ));

            validatePlayerBelongsToTeamInSeason(player, team, match.getSeason());
        }
        Optional<MatchTactics> existingTactics =
                matchTacticsRepository.findByMatchIdAndTeamId(matchId, teamId);

        boolean existedBefore = existingTactics.isPresent();

        MatchTactics tactics = existingTactics.orElseGet(() -> {
            MatchTactics newTactics = new MatchTactics();
            newTactics.setMatch(match);
            newTactics.setTeam(team);
            return newTactics;
        });

        tactics.setFormationName(request.getFormationName());
        tactics.setDescription(request.getDescription());

        MatchTactics savedTactics = matchTacticsRepository.save(tactics);

        matchLineupRepository.deleteByMatchTacticsId(savedTactics.getId());
        matchLineupRepository.flush();

        if (request.getLineups() != null) {
            for (MatchLineupRequest lineupRequest : request.getLineups()) {
                Player player = playerRepository.findById(lineupRequest.getPlayerId())
                        .orElseThrow(() -> new RuntimeException(
                                "Không tìm thấy cầu thủ id = " + lineupRequest.getPlayerId()
                        ));

                MatchLineup lineup = new MatchLineup();
                lineup.setMatchTactics(savedTactics);
                lineup.setPlayer(player);
                lineup.setPosition(lineupRequest.getPosition());
                lineup.setShirtNumber(lineupRequest.getShirtNumber());
                lineup.setIsStarting(Boolean.TRUE.equals(lineupRequest.getIsStarting()));
                lineup.setLineupOrder(lineupRequest.getLineupOrder());
                lineup.setRole(lineupRequest.getRole());

                matchLineupRepository.save(lineup);
            }
        }

        if (match.getSeason() != null) {
            playerStatsService.recalculateBySeason(match.getSeason().getId());
        }

        notifyAdminsAboutLineup(
                matchId,
                teamId,
                existedBefore
        );

        return toResponse(
                matchTacticsRepository.findWithLineupsById(savedTactics.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy tactics sau khi lưu"))
        );
    }

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
    }

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

    @Transactional
    public void deleteTactics(Long matchId, Long tacticsId) {
        MatchTactics tactics = matchTacticsRepository.findById(tacticsId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tactics id = " + tacticsId));

        if (tactics.getMatch() == null || !tactics.getMatch().getId().equals(matchId)) {
            throw new RuntimeException("Tactics id = " + tacticsId + " không thuộc trận đấu id = " + matchId);
        }

        Long seasonId = tactics.getMatch().getSeason() != null ? tactics.getMatch().getSeason().getId() : null;

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

//        if (rule.getMaxPlayers() != null && lineups.size() > rule.getMaxPlayers()) {
//            throw new RuntimeException(
//                    "Danh sách cầu thủ vượt quá giới hạn của bộ luật. Tối đa: "
//                            + rule.getMaxPlayers()
//            );
//        }
//
//        if (rule.getMinPlayers() != null && lineups.size() < rule.getMinPlayers()) {
//            throw new RuntimeException(
//                    "Danh sách cầu thủ chưa đạt tối thiểu theo bộ luật. Tối thiểu: "
//                            + rule.getMinPlayers()
//            );
//        }
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

    private MatchTacticsResponse toResponse(MatchTactics tactics) {
        Team team = tactics.getTeam();

//        List<MatchLineupResponse> lineupResponses = tactics.getLineups() == null
//                ? List.of()
//                : tactics.getLineups()
//                .stream()
//                .map(this::toLineupResponse)
//                .toList();
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

//    private MatchLineupResponse toLineupResponse(MatchLineup lineup) {
//        Player player = lineup.getPlayer();
//
//        return new MatchLineupResponse(
//                lineup.getId(),
//                player != null ? player.getId() : null,
//                player != null ? player.getName() : null,
//                player != null ? player.getAvatar() : null,
//                lineup.getPosition(),
//                lineup.getShirtNumber(),
//                lineup.getIsStarting(),
//                lineup.getLineupOrder(),
//                lineup.getRole()
//        );
//    }

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

    public MatchTacticsResponse getTeamLineup(Long matchId, Long teamId) {
        return matchTacticsRepository.findByMatchIdAndTeamId(matchId, teamId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException(
                        "Chưa có đội hình cho teamId = " + teamId + " trong matchId = " + matchId
                ));
    }

    @Transactional
    public void deleteTeamLineup(Long matchId, Long teamId) {
        MatchTactics tactics = matchTacticsRepository.findByMatchIdAndTeamId(matchId, teamId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy đội hình của teamId = " + teamId + " trong matchId = " + matchId
                ));

        Long seasonId = tactics.getMatch() != null && tactics.getMatch().getSeason() != null
                ? tactics.getMatch().getSeason().getId()
                : null;

        matchTacticsRepository.delete(tactics);

        if (seasonId != null) {
            playerStatsService.recalculateBySeason(seasonId);
        }

        sendLineupDeletedEventToAdmins(matchId);
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
}
