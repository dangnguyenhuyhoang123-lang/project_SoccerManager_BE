package com.example.demo.controller.team.player;

import com.example.demo.dto.player.PlayerSuspensionResponse;
import com.example.demo.service.team.player.PlayerSuspensionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/player-suspensions")
@RequiredArgsConstructor
@CrossOrigin
public class PlayerSuspensionController {

    private final PlayerSuspensionService playerSuspensionService;

    @GetMapping
    public List<PlayerSuspensionResponse> getBySeason(@RequestParam Long seasonId) {
        return playerSuspensionService.getBySeason(seasonId);
    }

    @GetMapping("/match/{matchId}/active")
    public List<PlayerSuspensionResponse> getActiveByMatch(@PathVariable Long matchId) {
        return playerSuspensionService.getActiveByMatch(matchId);
    }
}
