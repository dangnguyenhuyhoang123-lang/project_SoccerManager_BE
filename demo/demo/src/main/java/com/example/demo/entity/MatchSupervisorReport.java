package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "match_supervisor_report")
@Getter
@Setter
public class MatchSupervisorReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    @Column(name = "supervisor_name")
    private String supervisorName;

    @Column(columnDefinition = "TEXT")
    private String organizationReview;

    @Column(columnDefinition = "TEXT")
    private String refereeIssueNote;

    @Column(columnDefinition = "TEXT")
    private String playerIssueNote;

    @Column(columnDefinition = "TEXT")
    private String stadiumIssueNote;

    @Column(columnDefinition = "TEXT")
    private String disciplineRecommendation;
}
