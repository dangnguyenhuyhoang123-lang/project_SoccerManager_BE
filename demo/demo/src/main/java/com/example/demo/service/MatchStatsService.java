package com.example.demo.service;

import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.match.MatchStatsRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.dto.matchstats.MatchStatsResponse;
import com.example.demo.dto.matchstats.MatchStatsUpsertRequest;
import com.example.demo.entity.Match;
import com.example.demo.entity.MatchStats;
import com.example.demo.entity.SeasonTeam;
import com.example.demo.entity.Team;
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
public class MatchStatsService {

    private final MatchStatsRepository matchStatsRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final TeamStatsService teamStatsService;
    private final UserRepository userRepository;
    private final RealtimeEventService realtimeEventService;

    public List<MatchStatsResponse> getByMatch(Long matchId) {
        return matchStatsRepository.findByMatchId(matchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<MatchStatsResponse> upsertMatchStats(Long matchId, List<MatchStatsUpsertRequest> requests) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu id = " + matchId));

        for (MatchStatsUpsertRequest request : requests) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng id = " + request.getTeamId()));

            validateTeamBelongsToMatch(match, team);

            MatchStats stats = matchStatsRepository.findByMatchIdAndTeamId(matchId, team.getId())
                    .orElseGet(() -> {
                        MatchStats newStats = new MatchStats();
                        newStats.setMatch(match);
                        newStats.setTeam(team);
                        return newStats;
                    });

            applyRequest(stats, request);
            matchStatsRepository.save(stats);
        }

        Long seasonId = match.getSeason() != null ? match.getSeason().getId() : null;

        if (seasonId != null) {
            teamStatsService.recalculateBySeason(seasonId);
        }

        sendMatchStatsRealtimeEvents(match, seasonId);

        return getByMatch(matchId);
    }

    private void sendMatchStatsRealtimeEvents(Match match, Long seasonId) {
        Set<Long> userIds = findRelatedUserIds(match);

        realtimeEventService.sendToUsers(
                userIds,
                realtimeEvent("MATCH_STATS_UPDATED", match.getId(), "MATCH_STATS", "REFETCH_MATCH_STATS")
        );

        if (seasonId != null) {
            realtimeEventService.sendToUsers(
                    userIds,
                    realtimeEvent("TEAM_STATS_UPDATED", seasonId, "TEAM_STATS", "REFETCH_TEAM_STATS")
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

    private void validateTeamBelongsToMatch(Match match, Team team) {
        Long homeTeamId = match.getHomeTeam().getTeam().getId();
        Long awayTeamId = match.getAwayTeam().getTeam().getId();

        if (!team.getId().equals(homeTeamId) && !team.getId().equals(awayTeamId)) {
            throw new RuntimeException("Đội " + team.getName() + " không thuộc trận đấu này");
        }
    }

    private void applyRequest(MatchStats stats, MatchStatsUpsertRequest request) {
        stats.setPossession(request.getPossession());
        stats.setShots(request.getShots());
        stats.setShotsOnTarget(request.getShotsOnTarget());
        stats.setCorners(request.getCorners());
        stats.setFouls(request.getFouls());
        stats.setOffsides(request.getOffsides());
        stats.setYellowCards(request.getYellowCards());
        stats.setRedCards(request.getRedCards());
        stats.setTotalPasses(request.getTotalPasses());
        stats.setPassAccuracy(request.getPassAccuracy());
    }

    private MatchStatsResponse toResponse(MatchStats stats) {
        Team team = stats.getTeam();

        return new MatchStatsResponse(
                stats.getId(),
                stats.getMatch() != null ? stats.getMatch().getId() : null,
                team != null ? team.getId() : null,
                team != null ? team.getName() : null,
                team != null ? team.getLogo() : null,
                stats.getPossession(),
                stats.getShots(),
                stats.getShotsOnTarget(),
                stats.getCorners(),
                stats.getFouls(),
                stats.getOffsides(),
                stats.getYellowCards(),
                stats.getRedCards(),
                stats.getTotalPasses(),
                stats.getPassAccuracy()
        );
    }
}
