package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(
        name = "standing",
        uniqueConstraints = @UniqueConstraint(name = "uk_standing_season_team", columnNames = {"season_id", "team_id"}),
        indexes = {
                @Index(name = "idx_standing_season", columnList = "season_id"),
                @Index(name = "idx_standing_team", columnList = "team_id"),
                @Index(name = "idx_standing_points", columnList = "points")
        }
)
public class Standing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Integer played;

    @Column
    private Integer win;

    @Column
    private Integer draw;

    @Column
    private Integer lose;

    @Column(name = "goals_for")
    private Integer goalsFor;

    @Column(name = "goals_against")
    private Integer goalsAgainst;

    @Column(name = "goals_difference")
    private Integer goalDifference;

    @Column
    private Integer points;

    @Column
    private Integer rank;

    @Column(name = "recent_form")
    private String recentForm;

    @Column(name = "current_rank")
    private Integer currentRank;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    public void updateGoalDifference() {
        this.goalDifference = (this.goalsFor != null ? this.goalsFor : 0)
                - (this.goalsAgainst != null ? this.goalsAgainst : 0);
    }


}
