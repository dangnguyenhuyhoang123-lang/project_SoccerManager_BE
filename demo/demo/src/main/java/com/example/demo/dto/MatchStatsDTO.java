package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchStatsDTO {
    public Long matchId;
    public Long homeTeamId;
    public String homeTeamName;
    public Long awayTeamId;
    public String awayTeamName;

    public Integer shotsHome;
    public Integer shotsAway;

    public Integer shotsOnTargetHome;
    public Integer shotsOnTargetAway;

    public Float possessionHome;
    public Float possessionAway;

    public Integer cornersHome;
    public Integer cornersAway;

    public Integer foulsHome;
    public Integer foulsAway;

    public Integer offsidesHome;
    public Integer offsidesAway;

    public Integer yellowCardsHome;
    public Integer yellowCardsAway;

    public Integer redCardsHome;
    public Integer redCardsAway;

    public Integer totalPassesHome;
    public Integer totalPassesAway;

    public Float passAccuracyHome;
    public Float passAccuracyAway;
}
