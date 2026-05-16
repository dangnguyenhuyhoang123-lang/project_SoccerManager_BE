package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(
        name = "team",
        indexes = {
                @Index(name = "idx_team_name", columnList = "name"),
                @Index(name = "idx_team_city", columnList = "city"),
                @Index(name = "idx_team_status", columnList = "status")
        }
)
public class Team extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String logo;

    @Column
    private Integer establishedYear;

    @Column
    private String city;

    @Column
    private String region;

    @Column
    private String owner;

    private String description;

    @Column(nullable = false)
    private String status;




    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id") // Bỏ unique = true để nhiều đội dùng chung 1 sân
    private Stadium stadium;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private List<Player> players;


    @OneToMany(mappedBy = "team")
    private List<SeasonTeamCoach> coachesHistory;

    // Quan hệ với trận đấu (2 chiều từ phía Team)
    @OneToMany(mappedBy = "homeTeam")
    private List<Match> homeMatches;

    @OneToMany(mappedBy = "awayTeam")
    private List<Match> awayMatches;

    @OneToMany(mappedBy = "team")
    private List<SeasonTeam> participatingSeasons;
}
