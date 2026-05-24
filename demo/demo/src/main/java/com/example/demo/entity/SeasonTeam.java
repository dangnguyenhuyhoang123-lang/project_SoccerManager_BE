package com.example.demo.entity;

import com.example.demo.entity.registerclub.RegistrationTeam;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(
        name = "season_team",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_season_team_team_season", columnNames = {"team_id", "season_id"}),
                @UniqueConstraint(name = "uk_season_team_registration", columnNames = {"registration_id"})
        },
        indexes = {
                @Index(name = "idx_season_team_season", columnList = "season_id"),
                @Index(name = "idx_season_team_team", columnList = "team_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeasonTeam extends  BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // team tham gia mùa này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    // liên kết đến season (một season thuộc một league)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", unique = true)
    private RegistrationTeam registrationTeam;

    @OneToMany(mappedBy = "homeTeam")
    private List<Match> homeMatches;

    @OneToMany(mappedBy = "awayTeam")
    private List<Match> awayMatches;

    @Column
    private String notes;

    @Column(nullable = false)
    private String status;
}
