package com.example.demo.controller.season;

import com.example.demo.dto.MatchDTO;
import com.example.demo.dto.ScheduleGenerateRequest;
import com.example.demo.service.match.MatchService;
import com.example.demo.service.season.SeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/seasons")
@CrossOrigin
public class SeasonController {

    private final SeasonService seasonService;
    private final MatchService matchService;



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

    @PatchMapping("/{seasonId}/system-rule/{ruleId}")
    public SeasonResponse assignSystemRule(
            @PathVariable Long seasonId,
            @PathVariable Long ruleId
    ) {
        return seasonService.assignSystemRule(seasonId, ruleId);
    }

    @DeleteMapping("/deleteSeason/{id}")
    public void deleteSeason(@PathVariable Long id) {
        seasonService.delete(id);
    }

    @PostMapping("/{seasonId}/generate-schedule")
    public List<MatchDTO> generateSchedule(
            @PathVariable Long seasonId,
            @RequestBody ScheduleGenerateRequest request
    ) {
        return matchService.generateDoubleRoundRobinSchedule(seasonId, request);
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
