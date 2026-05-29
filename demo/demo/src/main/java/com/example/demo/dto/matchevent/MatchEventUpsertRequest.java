package com.example.demo.dto.matchevent;

import com.example.demo.entity.EventType;
import com.example.demo.entity.GoalType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchEventUpsertRequest {

    private Integer minute;
    private Integer extraMinute;
    private Integer eventOrder;

    private EventType eventType;
    private GoalType goalType;

    private Long teamId;

    private Long playerId;
    private Long playerInId;
    private Long assistPlayerId;

    private String note;
}
