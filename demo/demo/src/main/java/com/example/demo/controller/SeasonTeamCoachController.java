package com.example.demo.controller;

import com.example.demo.service.SeasonTeamCoachService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/season-team-coaches")
@CrossOrigin
public class SeasonTeamCoachController {

    private final SeasonTeamCoachService seasonTeamCoachService;

    @Autowired
    public SeasonTeamCoachController(SeasonTeamCoachService seasonTeamCoachService) {
        this.seasonTeamCoachService = seasonTeamCoachService;
    }

    @GetMapping("/getAssignments")
    public Page<SeasonTeamCoachResponse> getAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long coachId
    ) {
        return seasonTeamCoachService.getAssignments(page, size, seasonId, teamId, coachId);
    }

    @GetMapping("/getAssignment/{id}")
    public SeasonTeamCoachResponse getAssignment(@PathVariable Long id) {
        return seasonTeamCoachService.getAssignment(id);
    }

    @PostMapping("/addAssignment")
    public SeasonTeamCoachResponse createAssignment(@RequestBody SeasonTeamCoachRequest request) {
        return seasonTeamCoachService.create(request);
    }

    @PutMapping("/updateAssignment/{id}")
    public SeasonTeamCoachResponse updateAssignment(@PathVariable Long id, @RequestBody SeasonTeamCoachRequest request) {
        return seasonTeamCoachService.update(id, request);
    }

    @DeleteMapping("/deleteAssignment/{id}")
    public void deleteAssignment(@PathVariable Long id) {
        seasonTeamCoachService.delete(id);
    }

    public record SeasonTeamCoachRequest(
            Long seasonId,
            Long teamId,
            Long coachId,
            String role,
            LocalDate assignedDate,
            LocalDate endDate,
            String status
    ) {
    }

    public record SeasonTeamCoachResponse(
            Long id,
            Long seasonId,
            String seasonName,
            Long teamId,
            String teamName,
            Long coachId,
            String coachName,
            String role,
            LocalDate assignedDate,
            LocalDate endDate,
            String status
    ) {
    }
}
