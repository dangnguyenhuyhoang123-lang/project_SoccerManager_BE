package com.example.demo.controller;

import com.example.demo.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin
public class TeamController {

    private TeamService teamService;

    @Autowired
    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public Page<TeamResponse> getTeams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long seasonId
    ) {
        return teamService.getTeams(page, size, search, city, seasonId);
    }

    @GetMapping("/{id}")
    public TeamResponse getTeam(@PathVariable Long id) {
        return teamService.getTeam(id);
    }

    @PostMapping
    public TeamResponse createTeam(@RequestBody TeamRequest request) {
        return teamService.create(request);
    }

    @PutMapping("/{id}")
    public TeamResponse updateTeam(@PathVariable Long id, @RequestBody TeamRequest request) {
        return teamService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteTeam(@PathVariable Long id) {
        teamService.delete(id);
    }

    public record TeamRequest(
            String name,
            String logo,
            Integer establishedYear,
            String city,
            String region,
            String owner,
            String description,
            String status,
            Long stadiumId
    ) {
    }

    public record TeamResponse(
            Long id,
            String name,
            String logo,
            Integer establishedYear,
            String city,
            String region,
            String owner,
            String description,
            String status,
            Long stadiumId,
            String stadiumName
    ) {
    }
}
