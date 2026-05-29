package com.example.demo.dto.matchevent;

import com.example.demo.entity.EventType;
import com.example.demo.entity.GoalType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MatchEventResponse {

    private Long id;
    private Long matchId;

    private Integer minute;
    private Integer extraMinute;
    private Integer eventOrder;

    private EventType eventType;
    private GoalType goalType;

    private Long teamId;
    private String teamName;
    private String teamLogo;

    private Long playerId;
    private String playerName;

    private Long playerInId;
    private String playerInName;

    private Long assistPlayerId;
    private String assistPlayerName;

    private String note;
}