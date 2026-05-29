package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "team_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;

    @Column(name = "avg_goals_per_match")
    private Double avgGoalsPerMatch;

    @Column(name = "clean_sheets")
    private Integer cleanSheets;

    @Column(name = "possession_avg")
    private Double possessionAvg;

    @Column
    private Integer played;

    @Column(name = "total_goals")
    private Integer totalGoals;

    @Column(name = "total_conceded")
    private Integer totalConceded;
}