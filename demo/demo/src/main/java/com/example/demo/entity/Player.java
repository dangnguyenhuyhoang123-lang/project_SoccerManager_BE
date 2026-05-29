package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Table(
        name = "player",
        indexes = {
                @Index(name = "idx_player_name", columnList = "name"),
                @Index(name = "idx_player_id_code", columnList = "IDCode"),
                @Index(name = "idx_player_shirt", columnList = "shirtNumber"),
                @Index(name = "idx_player_position", columnList = "position"),
                @Index(name = "idx_player_status", columnList = "status")
        }
)
public class Player extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String IDCode;

    @Column(columnDefinition = "LONGTEXT")
    @Lob
    private String avatar;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column
    private String position;

    @Column
    private String detailPosition;

    @Column
    private Integer shirtNumber;

    @Column
    private String nationality;

    @Column Integer height;

    @Column Integer weight;


    @Column(nullable = false)
    private String status;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "normalize_name")
    private String normalizedName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @OneToMany(mappedBy = "player")
    private List<MatchEvent> events;

    @OneToMany(mappedBy = "player")
    private List<MatchLineup> matchMatchLineups;

    @Column(name = "vpf_player_slug", length = 255)
    private String vpfPlayerSlug;



}
