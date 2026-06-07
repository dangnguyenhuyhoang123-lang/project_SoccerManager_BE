package com.example.demo.dto;

import com.example.demo.entity.match.EventType;
import com.example.demo.entity.match.GoalType;
import lombok.Data;


@Data
public class MatchEventDTO {
    private Long id;
    private Integer minute;
    private EventType eventType;
    private GoalType goalType;

    private Long teamId;
    private Long playerId;
    private String playerName;

    private Long playerInId;
    private String playerInName;

    private Long assistPlayerId;
    private String assistPlayerName;

    private String teamName;

    private String note;
    private String description;
}
