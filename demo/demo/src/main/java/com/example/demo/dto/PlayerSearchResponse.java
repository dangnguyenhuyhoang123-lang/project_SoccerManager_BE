package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerSearchResponse {
    private Long playerId;
    private String playerName;

    private Long teamId;
    private String teamName;

    private Long seasonId;

    private String playerType;
    private String nationality;

    private Long totalGoals;
}
