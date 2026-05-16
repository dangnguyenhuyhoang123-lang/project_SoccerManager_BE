package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(
        name = "match_stats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "team_id"})
)
public class MatchStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    private Float possession;

    @Column
    private Integer shots;

    @Column(name = "shots_on_target")
    private Integer shotsOnTarget;

    @Column
    private Integer corners;

    @Column
    private Integer fouls;

    @Column
    private Integer offsides;

    @Column(name = "yellow_cards")
    private Integer yellowCards;

    @Column(name = "red_cards")
    private Integer redCards;

    @Column(name = "total_passes")
    private Integer totalPasses;

    @Column(name = "pass_accuracy")
    private Float passAccuracy;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
}
