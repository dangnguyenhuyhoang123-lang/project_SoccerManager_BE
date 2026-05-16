package com.example.demo.controller;

import com.example.demo.service.LeagueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leagues")
@CrossOrigin
public class LeagueController {

    private LeagueService leagueService;

    @Autowired
    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @GetMapping
    public Page<LeagueResponse> getLeagues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search
    ) {
        return leagueService.getLeagues(page, size, search);
    }

    @GetMapping("/{id}")
    public LeagueResponse getLeague(@PathVariable Long id) {
        return leagueService.getLeague(id);
    }

    @GetMapping("/{id}/seasons")
    public java.util.List<SeasonResponse> getLeagueSeasons(@PathVariable Long id) {
        return leagueService.getLeagueSeasons(id);
    }

    @PostMapping
    public LeagueResponse createLeague(@RequestBody LeagueRequest request) {
        return leagueService.create(request);
    }

    @PutMapping("/{id}")
    public LeagueResponse updateLeague(@PathVariable Long id, @RequestBody LeagueRequest request) {
        return leagueService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteLeague(@PathVariable Long id) {
        leagueService.delete(id);
    }

    public record LeagueRequest(String name, String country, String scale, String status, String logo) {
    }

    public record LeagueResponse(Long id, String name, String country, String scale, String status, String logo) {
    }

    public record SeasonResponse(
            Long id,
            String year,
            String name,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            Long leagueId,
            String leagueName,
            Long systemRuleId
    ) {
    }
}
