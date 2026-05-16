package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LineUpDTO {

    private Long id;

    private String playerName;

    private Integer shirtNumber;

    private String position;

    private Long teamID;

    private String teamName;

    private String avatar;

    private Boolean isStarting;

    private Integer lineupOrder;

    private String role;
}