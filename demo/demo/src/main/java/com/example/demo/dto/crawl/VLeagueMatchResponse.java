package com.example.demo.dto.crawl;

import com.example.demo.entity.MatchStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VLeagueMatchResponse {

    private Long id;
    private Integer vpfMatchCode;

    private Long seasonId;
    private String seasonName;
    private String seasonYear;

    private Integer roundNumber;
    private String roundName;

    private LocalDateTime matchDate;
    private MatchStatus status;

    private Long homeSeasonTeamId;
    private Long homeTeamId;
    private String homeTeamName;
    private String homeTeamLogo;

    private Long awaySeasonTeamId;
    private Long awayTeamId;
    private String awayTeamName;
    private String awayTeamLogo;

    private Integer homeScore;
    private Integer awayScore;

    private Integer stadiumId;
    private String stadiumName;

    private String broadcast;
    private Integer attendance;
    private String sourceUrl;
}
