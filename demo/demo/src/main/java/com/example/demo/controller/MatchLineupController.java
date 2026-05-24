package com.example.demo.controller;

import com.example.demo.dto.LineUpSubmit.MatchLineupSubmitDTO;
import com.example.demo.service.MatchLineupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lineups")
@CrossOrigin
public class MatchLineupController {

    private final MatchLineupService matchLineupService;

    @Autowired
    public MatchLineupController(MatchLineupService matchLineupService) {
        this.matchLineupService = matchLineupService;
    }

    @GetMapping("/match/{matchId}")
    public List<TeamLineupResponse> getLineupsByMatch(@PathVariable Long matchId) {
        return matchLineupService.getLineupsByMatch(matchId);
    }

    @GetMapping("/match/{matchId}/team/{teamId}")
    public TeamLineupResponse getLineupByMatchAndTeam(@PathVariable Long matchId, @PathVariable Long teamId) {
        return matchLineupService.getLineupByMatchAndTeam(matchId, teamId);
    }

    @GetMapping("/tactics/{tacticsId}")
    public TeamLineupResponse getLineupByTactics(@PathVariable Long tacticsId) {
        return matchLineupService.getLineupByTactics(tacticsId);
    }

    @PostMapping("/submit")
    public TeamLineupResponse submitLineup(@RequestBody MatchLineupSubmitDTO dto) {
        return matchLineupService.submitLineup(dto);
    }

    @DeleteMapping("/match/{matchId}/team/{teamId}")
    public void deleteLineup(@PathVariable Long matchId, @PathVariable Long teamId) {
        matchLineupService.deleteLineup(matchId, teamId);
    }

    public record TeamLineupResponse(
            Long tacticsId,
            Long matchId,
            Long teamId,
            String teamName,
            String formationName,
            String description,
            List<LineupPlayerResponse> players
    ) {
    }

    public record LineupPlayerResponse(
            Long lineupId,
            Long playerId,
            String playerName,
            String avatar,
            Integer shirtNumber,
            String position,
            Boolean isStarting,
            Integer lineupOrder,
            String role
    ) {
    }
}
