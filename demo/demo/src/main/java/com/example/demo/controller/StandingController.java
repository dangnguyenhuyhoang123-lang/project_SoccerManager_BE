package com.example.demo.controller;

import com.example.demo.service.StandingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/standings")
@CrossOrigin
public class StandingController {

    private StandingService standingService;

    @Autowired
    public StandingController(StandingService standingService) {
        this.standingService = standingService;
    }

    @GetMapping("/getStandingBySeason")
    public List<StandingResponse> getStandings(@RequestParam(required = false) Long seasonId) {
        return standingService.getStandings(seasonId);
    }

    @GetMapping("/getStanding/{id}")
    public StandingResponse getStanding(@PathVariable Long id) {
        return standingService.getStanding(id);
    }

    public record StandingResponse(
            Long id,
            Long seasonId,
            String seasonName,
            Long teamId,
            String teamName,
            Integer played,
            Integer win,
            Integer draw,
            Integer lose,
            Integer goalsFor,
            Integer goalsAgainst,
            Integer goalDifference,
            Integer points,
            Integer rank,
            Integer currentRank,
            String recentForm
    ) {
    }

    @PostMapping("/recalculate")
    public List<StandingResponse> recalculateStandings(@RequestParam Long seasonId) {
        return standingService.recalculateBySeason(seasonId);
    }
}
