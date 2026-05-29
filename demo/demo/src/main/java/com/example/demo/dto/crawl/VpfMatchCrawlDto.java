package com.example.demo.dto.crawl;

import com.example.demo.entity.MatchStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VpfMatchCrawlDto {

    private String leagueName;
    private String seasonYear;
    private String seasonName;

    private Integer roundNumber;
    private String roundName;

    private Integer vpfMatchCode;

    private LocalDateTime matchDate;

    private String stadiumName;

    private String homeTeamName;
    private String awayTeamName;

    private Integer homeScore;
    private Integer awayScore;

    private MatchStatus status;

    private String broadcast;

    private Integer attendance;

    private String sourceUrl;
}