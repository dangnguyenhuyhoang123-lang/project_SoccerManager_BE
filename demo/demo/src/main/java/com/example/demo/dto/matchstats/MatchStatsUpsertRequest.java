package com.example.demo.dto.matchstats;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchStatsUpsertRequest {
    private Long teamId;

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
