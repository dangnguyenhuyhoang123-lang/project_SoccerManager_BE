package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
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

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(nullable = false)
    private String status;




    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id") // Bỏ unique = true để nhiều đội dùng chung 1 sân
    private Stadium stadium;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private List<Player> players;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private List<Coach> coaches;



    @OneToMany(mappedBy = "team")
    private List<SeasonTeamCoach> coachesHistory;

    // Quan hệ với trận đấu (2 chiều từ phía Team)


    @OneToMany(mappedBy = "team")
    private List<SeasonTeam> participatingSeasons = new ArrayList<>();


    @Column(name = "vpf_team_id", length = 100)
    private String vpfTeamId;

    @Column(name = "sportsdb_team_id", length = 100)
    private String sportsDbTeamId;

    @Column(name = "normalized_name", length = 255)
    private String normalizedName;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(name = "source_url", length = 100)
    private String sourceUrl;
}
