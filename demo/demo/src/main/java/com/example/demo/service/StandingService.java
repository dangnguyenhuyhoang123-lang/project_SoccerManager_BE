package com.example.demo.service;

import com.example.demo.controller.StandingController;
import com.example.demo.dao.StandingRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;

import com.example.demo.entity.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class StandingService {

    private final StandingRepository standingRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;

    @Autowired
    public StandingService(StandingRepository standingRepository,
                           SeasonTeamRepository seasonTeamRepository,
                           SeasonRepository seasonRepository,
                           TeamRepository teamRepository,
                           MatchRepository matchRepository) {
        this.standingRepository = standingRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
    }

    public List<StandingController.StandingResponse> getStandings(Long seasonId) {
        List<Standing> standings = seasonId == null
                ? standingRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((Standing standing) -> standing.getSeason() != null ? standing.getSeason().getId() : Long.MAX_VALUE)
                        .thenComparing(Standing::getPoints, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList()
                : standingRepository.findBySeasonIdOrderByPointsDescGoalDifferenceDescGoalsForDesc(seasonId);

        return standings.stream()
                .map(this::toStandingResponse)
                .toList();
    }

    public StandingController.StandingResponse getStanding(Long id) {
        Standing standing = standingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Standing not found with id = " + id));
        return toStandingResponse(standing);
    }

    @Transactional
    public void initializeStanding(Long seasonId, Long teamId) {
        if (standingRepository.existsBySeasonIdAndTeamId(seasonId, teamId)) {
            return;
        }

        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải"));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng"));

        Standing standing = new Standing();
        standing.setSeason(season);
        standing.setTeam(team);
        standing.setPlayed(0);
        standing.setWin(0);
        standing.setDraw(0);
        standing.setLose(0);
        standing.setGoalsFor(0);
        standing.setGoalsAgainst(0);
        standing.setGoalDifference(0);
        standing.setPoints(0);
        standing.setCurrentRank(0);
        standing.setRecentForm("");

        standingRepository.save(standing);
    }

    private StandingController.StandingResponse toStandingResponse(Standing standing) {
        return new StandingController.StandingResponse(
                standing.getId(),
                standing.getSeason() != null ? standing.getSeason().getId() : null,
                standing.getSeason() != null ? standing.getSeason().getName() : null,
                standing.getTeam() != null ? standing.getTeam().getId() : null,
                standing.getTeam() != null ? standing.getTeam().getName() : null,
                standing.getPlayed(),
                standing.getWin(),
                standing.getDraw(),
                standing.getLose(),
                standing.getGoalsFor(),
                standing.getGoalsAgainst(),
                standing.getGoalDifference(),
                standing.getPoints(),
                standing.getRank(),
                standing.getCurrentRank(),
                standing.getRecentForm()
        );
    }

    @Transactional
    public List<StandingController.StandingResponse> recalculateBySeason(Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải"));

        // 1. Đảm bảo mọi đội tham gia mùa giải đều có Standing
        List<SeasonTeam> seasonTeams = seasonTeamRepository.findBySeasonId(seasonId);

        for (SeasonTeam seasonTeam : seasonTeams) {
            Team team = seasonTeam.getTeam();

            Standing standing = standingRepository.findBySeasonIdAndTeamId(seasonId, team.getId())
                    .orElseGet(() -> {
                        Standing newStanding = new Standing();
                        newStanding.setSeason(season);
                        newStanding.setTeam(team);
                        return newStanding;
                    });

            resetStanding(standing);
            standingRepository.save(standing);
        }

        // 2. Lấy tất cả trận đã kết thúc
        List<Match> finishedMatches = matchRepository.findBySeasonIdAndStatus(seasonId, MatchStatus.FINISHED);

        // 3. Cộng dữ liệu từng trận vào standing
        for (Match match : finishedMatches) {
            if (match.getHomeScore() == null || match.getAwayScore() == null) {
                continue;
            }

            Team homeTeam = match.getHomeTeam().getTeam();
            Team awayTeam = match.getAwayTeam().getTeam();

            Standing homeStanding = standingRepository.findBySeasonIdAndTeamId(seasonId, homeTeam.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy standing đội nhà: " + homeTeam.getName()));

            Standing awayStanding = standingRepository.findBySeasonIdAndTeamId(seasonId, awayTeam.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy standing đội khách: " + awayTeam.getName()));

            applyMatchResult(homeStanding, awayStanding, match.getHomeScore(), match.getAwayScore());

            standingRepository.save(homeStanding);
            standingRepository.save(awayStanding);
        }

        // 4. Sắp xếp và cập nhật rank
        List<Standing> sortedStandings = standingRepository
                .findBySeasonIdOrderByPointsDescGoalDifferenceDescGoalsForDesc(seasonId);

        int rank = 1;
        for (Standing standing : sortedStandings) {
            standing.setRank(rank);
            standing.setCurrentRank(rank);
            standingRepository.save(standing);
            rank++;
        }

        return getStandings(seasonId);
    }

    private void resetStanding(Standing standing) {
        standing.setPlayed(0);
        standing.setWin(0);
        standing.setDraw(0);
        standing.setLose(0);
        standing.setGoalsFor(0);
        standing.setGoalsAgainst(0);
        standing.setGoalDifference(0);
        standing.setPoints(0);
        standing.setRank(0);
        standing.setCurrentRank(0);
        standing.setRecentForm("");
    }

    private void applyMatchResult(Standing homeStanding,
                                  Standing awayStanding,
                                  Integer homeScore,
                                  Integer awayScore) {
        homeStanding.setPlayed(homeStanding.getPlayed() + 1);
        awayStanding.setPlayed(awayStanding.getPlayed() + 1);

        homeStanding.setGoalsFor(homeStanding.getGoalsFor() + homeScore);
        homeStanding.setGoalsAgainst(homeStanding.getGoalsAgainst() + awayScore);

        awayStanding.setGoalsFor(awayStanding.getGoalsFor() + awayScore);
        awayStanding.setGoalsAgainst(awayStanding.getGoalsAgainst() + homeScore);

        if (homeScore > awayScore) {
            homeStanding.setWin(homeStanding.getWin() + 1);
            homeStanding.setPoints(homeStanding.getPoints() + 3);
            homeStanding.setRecentForm(appendRecentForm(homeStanding.getRecentForm(), "W"));

            awayStanding.setLose(awayStanding.getLose() + 1);
            awayStanding.setRecentForm(appendRecentForm(awayStanding.getRecentForm(), "L"));
        } else if (homeScore < awayScore) {
            awayStanding.setWin(awayStanding.getWin() + 1);
            awayStanding.setPoints(awayStanding.getPoints() + 3);
            awayStanding.setRecentForm(appendRecentForm(awayStanding.getRecentForm(), "W"));

            homeStanding.setLose(homeStanding.getLose() + 1);
            homeStanding.setRecentForm(appendRecentForm(homeStanding.getRecentForm(), "L"));
        } else {
            homeStanding.setDraw(homeStanding.getDraw() + 1);
            homeStanding.setPoints(homeStanding.getPoints() + 1);
            homeStanding.setRecentForm(appendRecentForm(homeStanding.getRecentForm(), "D"));

            awayStanding.setDraw(awayStanding.getDraw() + 1);
            awayStanding.setPoints(awayStanding.getPoints() + 1);
            awayStanding.setRecentForm(appendRecentForm(awayStanding.getRecentForm(), "D"));
        }

        homeStanding.updateGoalDifference();
        awayStanding.updateGoalDifference();
    }

    private String appendRecentForm(String currentForm, String result) {
        String form = currentForm == null ? "" : currentForm;
        form = form + result;

        if (form.length() > 5) {
            form = form.substring(form.length() - 5);
        }

        return form;
    }
}
