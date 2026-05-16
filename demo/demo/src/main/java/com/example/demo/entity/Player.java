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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @OneToMany(mappedBy = "player")
    private List<MatchEvent> events;

    @OneToMany(mappedBy = "player")
    private List<MatchLineup> matchMatchLineups;

}
