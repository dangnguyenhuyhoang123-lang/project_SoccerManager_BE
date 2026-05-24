package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Data
@Table(
        name = "`match`",
        indexes = {
                @Index(name = "idx_match_season", columnList = "season_id"),
                @Index(name = "idx_match_round", columnList = "round_id"),
                @Index(name = "idx_match_time", columnList = "matchDate"),
                @Index(name = "idx_match_stadium", columnList = "stadium_id"),
                @Index(name = "idx_match_home_team", columnList = "home_team"),
                @Index(name = "idx_match_away_team", columnList = "away_team")
        }
)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name="away_score")
    private Integer awayScore;

    @Column
    private LocalDateTime matchDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id")
    private Stadium stadium;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team", nullable = false)
    private SeasonTeam homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team", nullable = false)
    private SeasonTeam awayTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;


    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchTactics> tactics;



    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    private List<MatchEvent> events;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    private List<MatchStats> stats;

}
