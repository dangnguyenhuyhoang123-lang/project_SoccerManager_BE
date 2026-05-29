package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name="league")
public class League extends  BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private String country;

    @Column
    private String scale;

    @Column
    private String status;

    @Column(columnDefinition = "LONGTEXT")
    @Lob
    private String logo;

    @OneToMany(mappedBy = "league", cascade = CascadeType.ALL)
    private List<Season> seasons;


    @Column(name = "vpf_league_slug", length = 255)
    private String vpfLeagueSlug;

    @Column(name = "sportsdb_league_id", length = 100)
    private String sportsDbLeagueId;

    @Column(name = "source_name", length = 100)
    private String sourceName;
}
