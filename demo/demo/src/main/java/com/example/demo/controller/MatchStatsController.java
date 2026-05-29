package com.example.demo.controller;

import com.example.demo.dto.matchstats.MatchStatsResponse;
import com.example.demo.dto.matchstats.MatchStatsUpsertRequest;
import com.example.demo.service.MatchStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchStatsController {

    private final MatchStatsService matchStatsService;

    @GetMapping("/{matchId}/stats")
    public List<MatchStatsResponse> getMatchStats(@PathVariable Long matchId) {
        return matchStatsService.getByMatch(matchId);
    }

    @PutMapping("/{matchId}/stats")
    public List<MatchStatsResponse> upsertMatchStats(
            @PathVariable Long matchId,
            @RequestBody List<MatchStatsUpsertRequest> requests
    ) {
        return matchStatsService.upsertMatchStats(matchId, requests);
    }
}
