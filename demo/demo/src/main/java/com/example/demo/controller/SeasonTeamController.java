package com.example.demo.controller;

import com.example.demo.service.SeasonTeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/season-teams")
@CrossOrigin
public class SeasonTeamController {

    private final SeasonTeamService seasonTeamService;

    @Autowired
    public SeasonTeamController(SeasonTeamService seasonTeamService) {
        this.seasonTeamService = seasonTeamService;
    }

    @GetMapping
    public Page<SeasonTeamResponse> getSeasonTeams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Long teamId
    ) {
        return seasonTeamService.getSeasonTeams(page, size, seasonId, teamId);
    }

    @GetMapping("/{id}")
    public SeasonTeamResponse getSeasonTeam(@PathVariable Long id) {
        return seasonTeamService.getSeasonTeam(id);
    }

    @PostMapping
    public SeasonTeamResponse createSeasonTeam(@RequestBody SeasonTeamRequest request) {
        return seasonTeamService.create(request);
    }

    @PutMapping("/{id}")
    public SeasonTeamResponse updateSeasonTeam(@PathVariable Long id, @RequestBody SeasonTeamRequest request) {
        return seasonTeamService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteSeasonTeam(@PathVariable Long id) {
        seasonTeamService.delete(id);
    }

    public record SeasonTeamRequest(
            Long seasonId,
            Long teamId,
            Long registrationId,
            String notes,
            String status
    ) {
    }

    public record SeasonTeamResponse(
            Long id,
            Long seasonId,
            String seasonName,
            Long teamId,
            String teamName,
            String city,
            String stadiumName,
            Long registrationId,
            String notes,
            String status
    ) {
    }
}
