package com.example.demo.dto.LineUpSubmit;

import lombok.Data;

import java.util.List;


@Data
public class MatchLineupSubmitDTO {

    private Long matchId;
    private Long teamId;
    private String formationName; // "4-3-3"
    private String description;
    private List<PlayerPositionDTO> players;
}
