package com.example.demo.dto.systemrule;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemRuleRequest {
    private String ruleName;
    private String description;

    private Integer maxTeams;
    private Integer minAge;
    private Integer maxAge;

    private Integer minPlayers;
    private Integer maxPlayers;

    private Integer winPoints;
    private Integer drawPoints;
    private Integer losePoints;

    private String allowedGoalTypes;
    private String status;

    private Integer maxSubstitution;
    private Integer minRegistrationPlayers;
    private Integer maxForeignPlayers;
}
