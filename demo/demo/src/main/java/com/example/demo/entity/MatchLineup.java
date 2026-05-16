package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lineup")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchLineup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tactics_id")
    private MatchTactics matchTactics;

    // cầu thủ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    // vị trí thi đấu trên sân (string hoặc chuẩn hóa)
    @Column
    private String position;

    @Column(name = "shirt_number")
    private Integer shirtNumber;

    // đá chính hay dự bị
    @Column(name = "is_starting")
    private Boolean isStarting;

    // số thứ tự (ví dụ để vẽ đội hình)
    @Column(name = "lineup_order")
    private Integer lineupOrder;

    // role: captain, vice-captain...
    @Column
    private String role;
}