package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "match_tactics")
public class MatchTactics extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "formation_name")
    private String formationName; // Ví dụ: "4-3-3", "3-5-2"

    @Column(columnDefinition = "TEXT")
    private String description; // Ghi chú chiến thuật của HLV cho trận này

    // Có thể thêm trường này để quản lý danh sách cầu thủ thuộc chiến thuật này
    @OneToMany(mappedBy = "matchTactics", cascade = CascadeType.ALL)
    private List<MatchLineup> lineups;
}