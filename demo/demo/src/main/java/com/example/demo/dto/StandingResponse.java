package com.example.demo.dto;

public record StandingResponse(
        Long id,
        Long seasonId,
        String seasonName,
        Long teamId,
        String teamName,
        Integer played,
        Integer win,
        Integer draw,
        Integer lose,
        Integer goalsFor,
        Integer goalsAgainst,
        Integer goalDifference,
        Integer points,
        Integer rank,
        Integer currentRank,
        String recentForm
) {
}
