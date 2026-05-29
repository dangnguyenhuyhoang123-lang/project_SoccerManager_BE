package com.example.demo.service;

import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.match.MatchStatsRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dto.matchstats.MatchStatsResponse;
import com.example.demo.dto.matchstats.MatchStatsUpsertRequest;
import com.example.demo.entity.Match;
import com.example.demo.entity.MatchStats;
import com.example.demo.entity.Team;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchStatsService {

    private final MatchStatsRepository matchStatsRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final TeamStatsService teamStatsService;

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

        if (match.getSeason() != null) {
            teamStatsService.recalculateBySeason(match.getSeason().getId());
        }

        return getByMatch(matchId);
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
