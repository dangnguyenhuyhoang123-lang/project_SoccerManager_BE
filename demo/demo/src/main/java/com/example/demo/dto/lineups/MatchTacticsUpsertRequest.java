package com.example.demo.dto.lineups;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchTacticsUpsertRequest {
    private String formationName;
    private String description;
    private List<MatchLineupRequest> lineups;
}