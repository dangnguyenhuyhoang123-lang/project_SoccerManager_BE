package com.example.demo.controller;

import com.example.demo.service.PlayerStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/player-stats")
@RequiredArgsConstructor
public class PlayerStatsController {

    private final PlayerStatsService playerStatsService;

//    @PostMapping("/recalculate")
//    public List<PlayerStatsService.PlayerStatsResponse> recalculate(@RequestParam Long seasonId) {
//        return playerStatsService.recalculateBySeason(seasonId);
//    }

    @PostMapping("/recalculate-from-events")
    public List<PlayerStatsService.PlayerStatsResponse> recalculateFromEvents(@RequestParam Long seasonId) {
        return playerStatsService.recalculateBySeason(seasonId);
    }

    @GetMapping
    public List<PlayerStatsService.PlayerStatsResponse> getBySeason(@RequestParam Long seasonId) {
        return playerStatsService.getBySeason(seasonId);
    }
}
