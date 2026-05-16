package com.example.demo.dto;

import com.example.demo.entity.EventType;
import com.example.demo.entity.Match;
import com.example.demo.entity.Player;
import com.example.demo.entity.Team;
import jakarta.persistence.*;
import lombok.Data;


@Data
public class MatchEventDTO {
    private Long id;
    private Integer minute;
    private EventType eventType;

    private Long playerId;
    private String playerName;

    private Long playerInId;
    private String playerInName;

    private Long playerOutId;
    private String playerOutName;

    private String teamName;

    private String description;
}
