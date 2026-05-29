package com.example.demo.dto.lineups;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchLineupRequest {
    private Long playerId;
    private String position;
    private Integer shirtNumber;
    private Boolean isStarting;
    private Integer lineupOrder;
    private String role;
}
