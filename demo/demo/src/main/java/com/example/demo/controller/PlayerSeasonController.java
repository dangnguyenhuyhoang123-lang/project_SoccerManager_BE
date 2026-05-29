package com.example.demo.controller;

import com.example.demo.service.PlayerSeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;

@RestController
@RequestMapping("/api/player-seasons")
@CrossOrigin
public class PlayerSeasonController {

    private final PlayerSeasonService playerSeasonService;

    @Autowired
    public PlayerSeasonController(PlayerSeasonService playerSeasonService) {
        this.playerSeasonService = playerSeasonService;
    }

    @GetMapping("/getPlayerSeasons")
    public Page<PlayerSeasonResponse> getPlayerSeasons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long playerId
    ) {
        return playerSeasonService.getPlayerSeasons(page, size, seasonId, teamId, playerId);
    }

    @GetMapping("/getPlayerSeason/{id}")
    public PlayerSeasonResponse getPlayerSeason(@PathVariable Long id) {
        return playerSeasonService.getPlayerSeason(id);
    }

    @GetMapping("/getPlayerSeasonsByTeam/{teamId}")
    public List<PlayerSeasonResponse> getPlayerSeasonsByTeamId(@PathVariable Long teamId) {
        return playerSeasonService.getPlayerSeasonsByTeamId(teamId);
    }

    @GetMapping("/getPlayerSeasonsByTeamSeason/{teamSeasonId}")
    public List<PlayerSeasonResponse> getPlayerSeasonsByTeamSeasonId(@PathVariable Long teamSeasonId) {
        return playerSeasonService.getPlayerSeasonsByTeamSeasonId(teamSeasonId);
    }

    @PostMapping("/PlayerSeason")
    public PlayerSeasonResponse createPlayerSeason(@RequestBody PlayerSeasonRequest request) {
        return playerSeasonService.create(request);
    }

    @PutMapping("/updatePlayerSeason/{id}")
    public PlayerSeasonResponse updatePlayerSeason(@PathVariable Long id, @RequestBody PlayerSeasonRequest request) {
        return playerSeasonService.update(id, request);
    }

    @DeleteMapping("/deletePlayerSeason/{id}")
    public void deletePlayerSeason(@PathVariable Long id) {
        playerSeasonService.delete(id);
    }

    public record PlayerSeasonRequest(
            Long playerId,
            Long teamId,
            Long seasonId,
            Long teamSeasonId,
            Integer shirtNumber,
            String primaryPosition,
            Date contractStart,
            Date contractEnd
    ) {
    }

    public record PlayerSeasonResponse(
            Long id,
            Long playerId,
            String playerName,
            String avatar,
            Long teamId,
            String teamName,
            Long seasonId,
            String seasonName,
            Long teamSeasonId,
            Integer shirtNumber,
            String primaryPosition,
            Date contractStart,
            Date contractEnd
    ) {
    }
}
