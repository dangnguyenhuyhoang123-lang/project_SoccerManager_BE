package com.example.demo.dto.lineups;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MatchTacticsResponse {
    private Long id;
    private Long matchId;
    private Long teamId;
    private String teamName;
    private String teamLogo;
    private String formationName;
    private String description;
    private List<MatchLineupResponse> lineups;
}
