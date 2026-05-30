package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchDTO {
    private Long id;
    private String status;
    private Integer homeScore;
    private Integer awayScore;
    private LocalDateTime matchDate;
    private StadiumDTO stadium;
    private RoundDTO round;
    private TeamDTO homeTeam;
    private TeamDTO awayTeam;
    private SeasonDTO season;
    private Integer predictedHomeScore;
    private Integer predictedAwayScore;
}
