package com.example.demo.dto.systemrule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SystemRuleResponse {
    private Long id;

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
