package com.example.demo.dto.lineups;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MatchLineupsResponse {
    private Long matchId;
    private MatchTacticsResponse home;
    private MatchTacticsResponse away;
}
