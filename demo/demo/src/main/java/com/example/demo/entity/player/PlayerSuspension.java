package com.example.demo.entity.player;

import com.example.demo.entity.match.Match;
import com.example.demo.entity.season.Season;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "player_suspension")
@Getter
@Setter
public class PlayerSuspension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_match_id")
    private Match sourceMatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspended_match_id")
    private Match suspendedMatch;

    @Column(nullable = false)
    private String reason; // TWO_YELLOWS, RED_CARD

    @Column(nullable = false)
    private Boolean served = false;
}
