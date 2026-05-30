package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.MatchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@CrossOrigin
public class MatchController {

    @Autowired
    private MatchService matchService;

//    @GetMapping
//    public List<MatchDTO> getAllMatches() {
//        return matchService.getAllMatches();
//    }
//    @GetMapping
//    public Page<MatchDTO> getMatches(Pageable pageable) {
//        return matchService.getMatches(pageable);
//    }
//    @GetMapping
//    public Page<MatchDTO> getMatchesByLeagueName(
//            @RequestParam String leagueName,
//            Pageable pageable
//    ) {
//        return matchService.getMatchesByLeague_Name(leagueName, pageable);
//    }
//
//    @GetMapping
//    public Page<MatchDTO> getMatchesBySession(
//            @RequestParam String session,
//            Pageable pageable
//    ) {
//        return matchService.getMatchesBySeason_Year(session, pageable);
//    }
//
//    @GetMapping
//    public Page<MatchDTO> getMatchesByLeagueNameAndSessionYear(
//            @RequestParam String leagueName,
//            @RequestParam String session,
//            Pageable pageable
//    ) {
//        return matchService.getMatchesLeague_NameAndSeason_Year(leagueName,session, pageable);
//    }

//    @GetMapping
//    public Page<MatchDTO> getMatches(
//            @RequestParam(required = false) String leagueName,
//            @RequestParam(required = false) String season,
//            Pageable pageable
//    ) {
//        boolean hasLeague = hasText(leagueName);
//        boolean hasSeason = hasText(season);
//
//        if (hasLeague && hasSeason) {
//            return matchService.getMatchesLeague_NameAndSeason_Year(leagueName, season, pageable);
//        }
//
//        if (hasLeague) {
//            return matchService.getMatchesByLeague_Name(leagueName, pageable);
//        }
//
//        if (hasSeason) {
//            return matchService.getMatchesBySeason_Year(season, pageable);
//        }
//
//        return matchService.getMatches(pageable);
//    }
//
//    @GetMapping("/{id}")
//    public MatchDTO getMatchById(@PathVariable Long id) {
//        return matchService.getMatchById(id);
//    }
//
//    @GetMapping("/{id}/stats")
//    public MatchStatsDTO getStats(@PathVariable Long id) {
//        return matchService.getMatchStats(id);
//    }
//
//    @GetMapping("/{id}/list-event")
//    public List<MatchEventDTO> getListEvemt(@PathVariable Long id)
//    {
//        return matchService.getEventsByMatch(id);
//    }
//
//    @GetMapping("/{id}/lineup")
//    public Page<LineUpDTO> getLineup(
//            @PathVariable Long id,
//            Pageable pageable
//    ) {
//        return matchService.getLineup(id, pageable);
//    }

    @GetMapping("/getAllMatches")
    public Page<MatchDTO> getAllMatches(
            Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        int MAX_SIZE = 50;

        int size = Math.min(pageable.getPageSize(), MAX_SIZE);
        Pageable safePageable = PageRequest.of(
                pageable.getPageNumber(),
                size,
                pageable.getSort().isSorted()
                        ? pageable.getSort()
                        : Sort.by("matchDate").descending()
        );

        return matchService.getAllMatches(
                safePageable.getPageNumber(),
                safePageable.getPageSize(),
                status,
                search
        );
    }

    @GetMapping("/{id}")
    public MatchDTO getMatchById(@PathVariable Long id) {
        return matchService.getMatchById(id);
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
