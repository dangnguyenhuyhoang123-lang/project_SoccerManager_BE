package com.example.demo.dto.matchsupervisor;

import lombok.Data;

@Data
public class MatchSupervisorReportRequest {
    private String supervisorName;
    private String organizationReview;
    private String refereeIssueNote;
    private String playerIssueNote;
    private String stadiumIssueNote;
    private String disciplineRecommendation;
}
