package com.example.demo.dto.matchstats;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MatchStatsResponse {
    private Long id;

    private Long matchId;

    private Long teamId;
    private String teamName;
    private String teamLogo;

    private Float possession;
    private Integer shots;
    private Integer shotsOnTarget;
    private Integer corners;
    private Integer fouls;
    private Integer offsides;
    private Integer yellowCards;
    private Integer redCards;
    private Integer totalPasses;
    private Float passAccuracy;
}
