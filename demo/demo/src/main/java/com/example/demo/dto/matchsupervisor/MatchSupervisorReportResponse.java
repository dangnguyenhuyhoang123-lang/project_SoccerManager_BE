package com.example.demo.dto.matchsupervisor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchSupervisorReportResponse {
    private Long id;
    private Long matchId;
    private String supervisorName;
    private String organizationReview;
    private String refereeIssueNote;
    private String playerIssueNote;
    private String stadiumIssueNote;
    private String disciplineRecommendation;
}
