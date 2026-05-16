package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "achievements")
@Data
public class Achievement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false)
    private String category; // Ví dụ: CHAMPION, TOP_SCORER, BEST_PLAYER

    @Column(name = "award_name")
    private String awardName; // Ví dụ: Chiếc giày vàng, Cúp vô địch

    @Column(name = "winner_id")
    private Long winnerId; // ID của Đội hoặc Cầu thủ

    @Column(name = "winner_type")
    private String winnerType; // Phân loại: 'TEAM' hoặc 'PLAYER'

    @Column
    private String note;
}
