package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "system_rule")
public class SystemRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    // Số đội tối đa trong giải
    @Column(name = "max_teams")
    private Integer maxTeams;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "min_players")
    private Integer minPlayers;

    @Column(name = "max_players")
    private Integer maxPlayers;

    @Column(name = "win_points")
    private Integer winPoints;

    @Column(name = "draw_points")
    private Integer drawPoints;

    @Column(name = "lose_points")
    private Integer losePoints;

    @Column(name = "allowed_goal_types")
    private String allowedGoalTypes;

    @Column
    private String status;

    @Column(name = "max_substitution") // Số lượt thay người tối đa trong trận
    private Integer maxSubstitution;

    @Column(name = "min_registration_players") // Số cầu thủ tối thiểu để được tham gia giải
    private Integer minRegistrationPlayers;

    @Column(name = "max_foreign_players")
    private Integer maxForeignPlayers; // Mặc định là 3
}
