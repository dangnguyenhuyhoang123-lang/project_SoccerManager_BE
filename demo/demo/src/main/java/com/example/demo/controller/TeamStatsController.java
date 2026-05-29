package com.example.demo.controller;

import com.example.demo.service.TeamStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-stats")

public class TeamStatsController {

    private TeamStatsService teamStatsService;

    @Autowired
    public TeamStatsController(TeamStatsService teamStatsService) {
        this.teamStatsService = teamStatsService;
    }

    @PostMapping("/recalculate")
    public List<TeamStatsService.TeamStatsResponse> recalculate(@RequestParam Long seasonId) {
        return teamStatsService.recalculateBySeason(seasonId);
    }

    @GetMapping
    public List<TeamStatsService.TeamStatsResponse> getBySeason(@RequestParam Long seasonId) {
        return teamStatsService.getBySeason(seasonId);
    }
}
