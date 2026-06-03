package com.example.demo.controller;

import com.example.demo.dto.matchsupervisor.MatchSupervisorReportRequest;
import com.example.demo.dto.matchsupervisor.MatchSupervisorReportResponse;
import com.example.demo.service.MatchSupervisorReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches/{matchId}/supervisor-report")
@RequiredArgsConstructor
@CrossOrigin
public class MatchSupervisorReportController {

    private final MatchSupervisorReportService reportService;

    @GetMapping
    public MatchSupervisorReportResponse getByMatch(@PathVariable Long matchId) {
        return reportService.getByMatch(matchId);
    }

    @PutMapping
    public MatchSupervisorReportResponse upsert(
            @PathVariable Long matchId,
            @RequestBody MatchSupervisorReportRequest request
    ) {
        return reportService.upsert(matchId, request);
    }
}