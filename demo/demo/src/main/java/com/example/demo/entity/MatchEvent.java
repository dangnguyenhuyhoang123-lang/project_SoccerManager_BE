package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "match_event")
public class MatchEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer minute;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType; // GOAL, CARD, SUBSTITUTION, TIMEOUT...

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type")
    private GoalType goalType; // NORMAL, PENALTY, OWN_GOAL

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team; // Đội thực hiện hành động

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player; // Cầu thủ chính (Ghi bàn, Nhận thẻ, hoặc người Rời sân)

    // Dùng cho sự kiện Thay người
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_in_id")
    private Player playerIn;

    // Dùng cho sự kiện Kiến tạo (Nếu bạn muốn làm sâu hơn)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assist_player_id")
    private Player assistPlayer;

    @Column(length = 500)
    private String note; // Ghi chú thêm (Ví dụ: "Lỗi phản ứng", "Sút xa")
}