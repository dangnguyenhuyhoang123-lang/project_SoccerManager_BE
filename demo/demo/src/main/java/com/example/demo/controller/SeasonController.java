package com.example.demo.controller;

import com.example.demo.service.SeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/seasons")
@CrossOrigin
public class SeasonController {

    private SeasonService seasonService;

    @Autowired
    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping("/getAllSeasons")
    public Page<SeasonResponse> getSeasons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long leagueId
    ) {
        return seasonService.getSeasons(page, size, leagueId);
    }

    @GetMapping("/getSeason/{id}")
    public SeasonResponse getSeason(@PathVariable Long id) {
        return seasonService.getSeason(id);
    }

    @GetMapping("/getSeasonTeams/{id}")
    public java.util.List<SeasonTeamResponse> getSeasonTeams(@PathVariable Long id) {
        return seasonService.getSeasonTeams(id);
    }

    @PostMapping("/addSeason")
    public SeasonResponse createSeason(@RequestBody SeasonRequest request) {
        return seasonService.create(request);
    }

    @PutMapping("/updateSeason/{id}")
    public SeasonResponse updateSeason(@PathVariable Long id, @RequestBody SeasonRequest request) {
        return seasonService.update(id, request);
    }

    @DeleteMapping("/deleteSeason/{id}")
    public void deleteSeason(@PathVariable Long id) {
        seasonService.delete(id);
    }

    public record SeasonRequest(
            String year,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            Long leagueId,
            Long systemRuleId
    ) {
    }

    public record SeasonResponse(
            Long id,
            String year,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            Long leagueId,
            String leagueName,
            Long systemRuleId
    ) {
    }

    public record SeasonTeamResponse(
            Long id,
            Long teamId,
            String teamName,
            String city,
            String stadiumName,
            String status
    ) {
    }
}
