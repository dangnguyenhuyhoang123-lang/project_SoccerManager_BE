package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(
        name = "player_stats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "season_id"})
)
public class PlayerStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "int default 0")
    private Integer goals = 0;

    @Column(columnDefinition = "int default 0")
    private Integer assists = 0;

    @Column(name = "appearances", columnDefinition = "int default 0")
    private Integer appearances = 0; // Số trận ra sân

    @Column(name = "minutes_played", columnDefinition = "int default 0")
    private Integer minutesPlayed = 0;

    @Column(name = "yellow_cards", columnDefinition = "int default 0")
    private Integer yellowCards = 0;

    @Column(name = "red_cards", columnDefinition = "int default 0")
    private Integer redCards = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;
}