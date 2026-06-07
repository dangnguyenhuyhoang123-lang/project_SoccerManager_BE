package com.example.demo.controller.match;

import com.example.demo.dto.*;
import com.example.demo.service.match.MatchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@CrossOrigin
public class MatchController {

    @Autowired
    private MatchService matchService;



    @GetMapping("/getAllMatches")
    public Page<MatchDTO> getAllMatches(
            Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Integer roundId,
            @RequestParam(required = false) Long teamId
    ) {
        return matchService.getAllMatches(pageable, status, search, seasonId, roundId, teamId);
    }

    @GetMapping("/{id}")
    public MatchDTO getMatchById(@PathVariable Long id) {
        return matchService.getMatchById(id);
    }

    @GetMapping("/man-of-the-match-stats")
    public List<ManOfTheMatchStatsResponse> getManOfTheMatchStats(
            @RequestParam Long seasonId
    ) {
        return matchService.getManOfTheMatchStats(seasonId);
    }


    @PostMapping("/addMatch")
    public MatchDTO addMatch(@RequestBody MatchUpsertDTO match)
    {
        return matchService.save(match);
    }

    @PutMapping("/updateMatch/{id}")
    public MatchDTO updateMatch(@PathVariable Long id, @RequestBody MatchUpsertDTO match)
    {
        return matchService.update(id,match);
    }

    @PatchMapping("/{id}/status")
    public MatchDTO updateMatchStatus(
            @PathVariable Long id,
            @RequestBody MatchStatusUpdateDTO request
    ) {
        return matchService.updateStatus(id, request);
    }

    @PatchMapping("/{matchId}/man-of-the-match/{playerId}")
    public MatchDTO updateManOfTheMatch(
            @PathVariable Long matchId,
            @PathVariable Long playerId
    ) {
        return matchService.updateManOfTheMatch(matchId, playerId);
    }

    @DeleteMapping("/deleteMatch/{id}")
    public void deleteMatch(@PathVariable Long id)
    {
        matchService.delete(id);
    }

    @GetMapping("/{matchId}/teams/{teamId}/team-season")
    public ResponseEntity<MatchTeamSeasonDTO> getTeamSeasonByMatchAndTeam(
            @PathVariable Long matchId,
            @PathVariable Long teamId
    ) {
        return ResponseEntity.ok(
                matchService.getTeamSeasonByMatchAndTeam(matchId, teamId)
        );
    }

    @PostMapping("/{matchId}/predict")
    public MatchDTO predictMatchScore(@PathVariable Long matchId) {
        return matchService.predictMatchScore(matchId);
    }

}
