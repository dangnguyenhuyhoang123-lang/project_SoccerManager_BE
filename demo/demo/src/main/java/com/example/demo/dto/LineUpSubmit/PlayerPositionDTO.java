package com.example.demo.dto.LineUpSubmit;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class PlayerPositionDTO {
    private Long playerId;

    private String position;


    private Integer shirtNumber;

    private Boolean isStarting;


    private Integer lineupOrder;

    private String role;
}
