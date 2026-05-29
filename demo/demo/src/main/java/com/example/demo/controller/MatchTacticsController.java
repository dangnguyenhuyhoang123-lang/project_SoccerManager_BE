package com.example.demo.controller;

import com.example.demo.dto.lineups.MatchLineupsResponse;
import com.example.demo.dto.lineups.MatchTacticsResponse;
import com.example.demo.dto.lineups.MatchTacticsUpsertRequest;
import com.example.demo.service.MatchTacticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchTacticsController {

    private final MatchTacticsService matchTacticsService;

    @GetMapping("/{matchId}/teams/{teamId}/lineup")
    public MatchTacticsResponse getTeamLineup(
            @PathVariable Long matchId,
            @PathVariable Long teamId
    ) {
        return matchTacticsService.getTeamLineup(matchId, teamId);
    }



    @PutMapping("/{matchId}/teams/{teamId}/lineup")
    public MatchTacticsResponse upsertTeamLineup(
            @PathVariable Long matchId,
            @PathVariable Long teamId,
            @RequestBody MatchTacticsUpsertRequest request
    ) {
        return matchTacticsService.upsertTeamLineup(matchId, teamId, request);
    }

    @DeleteMapping("/{matchId}/teams/{teamId}/lineup")
    public void deleteTeamLineup(
            @PathVariable Long matchId,
            @PathVariable Long teamId
    ) {
        matchTacticsService.deleteTeamLineup(matchId, teamId);
    }

    @GetMapping("/{matchId}/tactics")
    public List<MatchTacticsResponse> getMatchTactics(@PathVariable Long matchId) {
        return matchTacticsService.getByMatch(matchId);
    }

    @GetMapping("/{matchId}/lineups")
    public MatchLineupsResponse getMatchLineups(@PathVariable Long matchId) {
        return matchTacticsService.getLineupsByMatch(matchId);
    }
}
