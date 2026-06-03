package com.example.demo.service;

import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.match.MatchSupervisorReportRepository;
import com.example.demo.dto.matchsupervisor.MatchSupervisorReportRequest;
import com.example.demo.dto.matchsupervisor.MatchSupervisorReportResponse;
import com.example.demo.entity.Match;
import com.example.demo.entity.MatchSupervisorReport;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchSupervisorReportService {

    private final MatchSupervisorReportRepository reportRepository;
    private final MatchRepository matchRepository;

    public MatchSupervisorReportResponse getByMatch(Long matchId) {
        MatchSupervisorReport report = reportRepository.findByMatchId(matchId)
                .orElse(null);

        if (report == null) {
            return null;
        }

        return toResponse(report);
    }

    @Transactional
    public MatchSupervisorReportResponse upsert(Long matchId, MatchSupervisorReportRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu id = " + matchId));

        MatchSupervisorReport report = reportRepository.findByMatchId(matchId)
                .orElseGet(MatchSupervisorReport::new);

        report.setMatch(match);
        report.setSupervisorName(request.getSupervisorName());
        report.setOrganizationReview(request.getOrganizationReview());
        report.setRefereeIssueNote(request.getRefereeIssueNote());
        report.setPlayerIssueNote(request.getPlayerIssueNote());
        report.setStadiumIssueNote(request.getStadiumIssueNote());
        report.setDisciplineRecommendation(request.getDisciplineRecommendation());

        return toResponse(reportRepository.save(report));
    }

    private MatchSupervisorReportResponse toResponse(MatchSupervisorReport report) {
        return new MatchSupervisorReportResponse(
                report.getId(),
                report.getMatch() != null ? report.getMatch().getId() : null,
                report.getSupervisorName(),
                report.getOrganizationReview(),
                report.getRefereeIssueNote(),
                report.getPlayerIssueNote(),
                report.getStadiumIssueNote(),
                report.getDisciplineRecommendation()
        );
    }
}
