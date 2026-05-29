package com.example.demo.service;

import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamStatsRepository;
import com.example.demo.entity.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service

public class TeamStatsService {

    private TeamStatsRepository teamStatsRepository;
    private SeasonRepository seasonRepository;
    private SeasonTeamRepository seasonTeamRepository;
    private MatchRepository matchRepository;

    public TeamStatsService() {
    }

    @Autowired
    public TeamStatsService(TeamStatsRepository teamStatsRepository, SeasonRepository seasonRepository, SeasonTeamRepository seasonTeamRepository, MatchRepository matchRepository) {
        this.teamStatsRepository = teamStatsRepository;
        this.seasonRepository = seasonRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional
    public List<TeamStatsResponse> recalculateBySeason(Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải"));

        List<SeasonTeam> seasonTeams = seasonTeamRepository.findBySeasonId(seasonId);

        Map<Long, TeamStatsAccumulator> statsMap = new LinkedHashMap<>();

        for (SeasonTeam seasonTeam : seasonTeams) {
            Team team = seasonTeam.getTeam();

            TeamStatsAccumulator accumulator = new TeamStatsAccumulator();
            accumulator.team = team;
            statsMap.put(team.getId(), accumulator);
        }

        List<Match> finishedMatches = matchRepository.findBySeasonIdAndStatus(seasonId, MatchStatus.FINISHED);

        for (Match match : finishedMatches) {
            if (match.getHomeScore() == null || match.getAwayScore() == null) {
                continue;
            }

            Team homeTeam = match.getHomeTeam().getTeam();
            Team awayTeam = match.getAwayTeam().getTeam();

            TeamStatsAccumulator homeStats = statsMap.get(homeTeam.getId());
            TeamStatsAccumulator awayStats = statsMap.get(awayTeam.getId());

            if (homeStats == null || awayStats == null) {
                continue;
            }

            applyMatchToTeamStats(homeStats, match.getHomeScore(), match.getAwayScore());
            applyMatchToTeamStats(awayStats, match.getAwayScore(), match.getHomeScore());
        }

        for (TeamStatsAccumulator accumulator : statsMap.values()) {
            TeamStats teamStats = teamStatsRepository.findBySeasonAndTeam(season, accumulator.team)
                    .orElseGet(() -> {
                        TeamStats newStats = new TeamStats();
                        newStats.setSeason(season);
                        newStats.setTeam(accumulator.team);
                        return newStats;
                    });

            teamStats.setTotalGoals(accumulator.totalGoals);
            teamStats.setTotalConceded(accumulator.totalConceded);
            teamStats.setCleanSheets(accumulator.cleanSheets);

            if (accumulator.played > 0) {
                teamStats.setAvgGoalsPerMatch(round2((double) accumulator.totalGoals / accumulator.played));
            } else {
                teamStats.setAvgGoalsPerMatch(0.0);
            }

            teamStats.setPlayed(accumulator.played);
            // Chưa có dữ liệu MatchStats nên tạm để null
            teamStats.setPossessionAvg(null);

            teamStatsRepository.save(teamStats);
        }

        return getBySeason(seasonId);
    }

    public List<TeamStatsResponse> getBySeason(Long seasonId) {
        return teamStatsRepository.findBySeasonId(seasonId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void applyMatchToTeamStats(TeamStatsAccumulator stats, Integer goalsFor, Integer goalsAgainst) {
        stats.played++;
        stats.totalGoals += goalsFor;
        stats.totalConceded += goalsAgainst;

        if (goalsAgainst == 0) {
            stats.cleanSheets++;
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private TeamStatsResponse toResponse(TeamStats stats) {
        return new TeamStatsResponse(
                stats.getId(),
                stats.getSeason() != null ? stats.getSeason().getId() : null,
                stats.getSeason() != null ? stats.getSeason().getName() : null,
                stats.getTeam() != null ? stats.getTeam().getId() : null,
                stats.getTeam() != null ? stats.getTeam().getName() : null,
                stats.getPlayed(),
                stats.getAvgGoalsPerMatch(),
                stats.getCleanSheets(),
                stats.getPossessionAvg(),
                stats.getTotalGoals(),
                stats.getTotalConceded()
        );
    }

    private static class TeamStatsAccumulator {
        private Team team;
        private int played = 0;
        private int totalGoals = 0;
        private int totalConceded = 0;
        private int cleanSheets = 0;
    }

    public record TeamStatsResponse(
            Long id,
            Long seasonId,
            String seasonName,
            Long teamId,
            String teamName,
            Integer played,
            Double avgGoalsPerMatch,
            Integer cleanSheets,
            Double possessionAvg,
            Integer totalGoals,
            Integer totalConceded
    ) {
    }
}
