package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManOfTheMatchStatsResponse {
    private Long playerId;
    private String playerName;
    private Long seasonId;
    private Long awardCount;
}
