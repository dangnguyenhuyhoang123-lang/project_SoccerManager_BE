package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "season_team_coach")
public class SeasonTeamCoach  extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id", nullable = false)
    private Coach coach;

    private String role; // Ví dụ: HLV Trưởng, Trợ lý, HLV Thủ môn

    private LocalDate assignedDate; // Ngày bắt đầu dẫn dắt

    private LocalDate endDate;      // Ngày rời đi (null nếu đang làm việc)

    private String status;
}
