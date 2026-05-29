package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "external_team_mapping",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"source_name", "external_team_id"})
        }
)
public class ExternalTeamMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sourceName; // VPF, TheSportsDB

    private String externalTeamId;

    private String externalTeamName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
}
