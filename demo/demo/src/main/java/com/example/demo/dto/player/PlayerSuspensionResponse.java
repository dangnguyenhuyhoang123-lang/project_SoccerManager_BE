package com.example.demo.dto.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerSuspensionResponse {
    private Long id;

    private Long playerId;
    private String playerName;

    private Long seasonId;

    private Long sourceMatchId;
    private Long suspendedMatchId;

    private String reason;
    private Boolean served;
}
