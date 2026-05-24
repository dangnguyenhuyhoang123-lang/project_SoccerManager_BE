package com.example.demo.dto;

import com.example.demo.entity.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchUpsertDTO {
    private MatchStatus status;
    private Integer homeScore;
    private Integer awayScore;
    private LocalDateTime matchDate;
    private Long stadiumId;
    private Long seasonId;
    private Long homeTeamId;
    private Long awayTeamId;
    private Integer roundId;
}
