package com.example.demo.controller;

import com.example.demo.service.RoundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/rounds")
@CrossOrigin
public class RoundController {

    private RoundService roundService;

    @Autowired
    public RoundController(RoundService roundService) {
        this.roundService = roundService;
    }

    @GetMapping
    public Page<RoundResponse> getRounds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long seasonId
    ) {
        return roundService.getRounds(page, size, seasonId);
    }

    @GetMapping("/{id}")
    public RoundResponse getRound(@PathVariable Integer id) {
        return roundService.getRound(id);
    }

    @PostMapping
    public RoundResponse createRound(@RequestBody RoundRequest request) {
        return roundService.create(request);
    }

    @PutMapping("/{id}")
    public RoundResponse updateRound(@PathVariable Integer id, @RequestBody RoundRequest request) {
        return roundService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteRound(@PathVariable Integer id) {
        roundService.delete(id);
    }

    public record RoundRequest(
            Integer roundNumber,
            String name,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer maxMatches,
            String status,
            Boolean notifyTeams,
            Long seasonId
    ) {
    }

    public record RoundResponse(
            Integer id,
            Integer roundNumber,
            String name,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer maxMatches,
            String status,
            Boolean notifyTeams,
            Long seasonId,
            String seasonName
    ) {
    }
}
