package com.example.demo.service;

import com.example.demo.controller.StandingController;
import com.example.demo.dao.StandingRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.entity.Season;
import com.example.demo.entity.Standing;
import com.example.demo.entity.Team;
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

    @Autowired
    public StandingService(StandingRepository standingRepository,
                           SeasonTeamRepository seasonTeamRepository,
                           SeasonRepository seasonRepository,
                           TeamRepository teamRepository) {
        this.standingRepository = standingRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
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
}
