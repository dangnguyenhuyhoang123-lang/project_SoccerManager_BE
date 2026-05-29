package com.example.demo.dto;



import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchTeamSeasonDTO {
    private Long matchId;
    private Long teamId;
    private Long seasonId;
    private Long teamSeasonId;
}
