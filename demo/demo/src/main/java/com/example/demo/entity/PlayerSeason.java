package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Entity
@Table(
        name = "player_season",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_player_season_player", columnNames = {"team_id", "season_id", "player_id"}),
                @UniqueConstraint(name = "uk_player_season_shirt", columnNames = {"team_id", "season_id", "shirt_number"})
        },
        indexes = {
                @Index(name = "idx_player_season_season", columnList = "season_id"),
                @Index(name = "idx_player_season_team", columnList = "team_id"),
                @Index(name = "idx_player_season_team_season", columnList = "team_season_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSeason {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_season_id", nullable = false)
    private SeasonTeam teamSeason;

    // số áo mùa đó
    @Column(name = "shirt_number", nullable = false)
    private Integer shirtNumber;

    // vị trí chính trong mùa này (GK, DF, MF, FW)
    @Column(name = "primary_position")
    private String primaryPosition;

    // hợp đồng: from/to

    @Column(name="contract_start")
    private Date contractStart;

    @Column(name = "contract_end")
    private Date contractEnd;

    @Column(nullable = false)
    private String status;
}
