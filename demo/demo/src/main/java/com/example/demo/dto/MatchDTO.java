package com.example.demo.dto;

import com.example.demo.entity.Season;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MatchDTO {
    private Long id;
    private String status;
    private Integer homeScore;
    private Integer awayScore;
    private LocalDateTime matchDate;

    private TeamDTO homeTeam;
    private TeamDTO awayTeam;
    private SeasonDTO season;
}