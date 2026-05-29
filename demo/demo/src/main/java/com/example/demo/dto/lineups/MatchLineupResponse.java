package com.example.demo.dto.lineups;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MatchLineupResponse {
    private Long id;
    private Long playerId;
    private String playerName;
    private String avatar;
    private String position;
    private Integer shirtNumber;
    private Boolean isStarting;
    private Integer lineupOrder;
    private String role;

}
