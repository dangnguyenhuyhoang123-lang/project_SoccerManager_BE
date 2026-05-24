package com.example.demo.service;

import com.example.demo.controller.SeasonTeamCoachController;
import com.example.demo.dao.CoachRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamCoachRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.entity.Coach;
import com.example.demo.entity.Season;
import com.example.demo.entity.SeasonTeamCoach;
import com.example.demo.entity.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SeasonTeamCoachService {

    private final SeasonTeamCoachRepository seasonTeamCoachRepository;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final CoachRepository coachRepository;

    @Autowired
    public SeasonTeamCoachService(SeasonTeamCoachRepository seasonTeamCoachRepository,
                                  SeasonRepository seasonRepository,
                                  TeamRepository teamRepository,
                                  CoachRepository coachRepository) {
        this.seasonTeamCoachRepository = seasonTeamCoachRepository;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
        this.coachRepository = coachRepository;
    }

    public Page<SeasonTeamCoachController.SeasonTeamCoachResponse> getAssignments(
            int page,
            int size,
            Long seasonId,
            Long teamId,
            Long coachId
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        if (seasonId != null && teamId != null) {
            return seasonTeamCoachRepository.findBySeasonIdAndTeamId(seasonId, teamId, pageable)
                    .map(this::toAssignmentResponse);
        }
        if (seasonId != null) {
            return seasonTeamCoachRepository.findBySeasonId(seasonId, pageable)
                    .map(this::toAssignmentResponse);
        }
        if (teamId != null) {
            return seasonTeamCoachRepository.findByTeamId(teamId, pageable)
                    .map(this::toAssignmentResponse);
        }
        if (coachId != null) {
            return seasonTeamCoachRepository.findByCoachId(coachId, pageable)
                    .map(this::toAssignmentResponse);
        }

        return seasonTeamCoachRepository.findAll(pageable)
                .map(this::toAssignmentResponse);
    }

    public SeasonTeamCoachController.SeasonTeamCoachResponse getAssignment(Long id) {
        return toAssignmentResponse(findAssignmentEntity(id));
    }

    public SeasonTeamCoachController.SeasonTeamCoachResponse create(SeasonTeamCoachController.SeasonTeamCoachRequest request) {
        SeasonTeamCoach assignment = new SeasonTeamCoach();
        applyRequest(assignment, request);
        return toAssignmentResponse(seasonTeamCoachRepository.save(assignment));
    }

    public SeasonTeamCoachController.SeasonTeamCoachResponse update(Long id, SeasonTeamCoachController.SeasonTeamCoachRequest request) {
        SeasonTeamCoach assignment = findAssignmentEntity(id);
        applyRequest(assignment, request);
        return toAssignmentResponse(seasonTeamCoachRepository.save(assignment));
    }

    public void delete(Long id) {
        if (!seasonTeamCoachRepository.existsById(id)) {
            throw new ResourceNotFoundException("Season team coach assignment not found with id = " + id);
        }
        seasonTeamCoachRepository.deleteById(id);
    }

    private SeasonTeamCoach findAssignmentEntity(Long id) {
        return seasonTeamCoachRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Season team coach assignment not found with id = " + id));
    }

    private void applyRequest(SeasonTeamCoach assignment, SeasonTeamCoachController.SeasonTeamCoachRequest request) {
        Season season = seasonRepository.findById(request.seasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Season not found with id = " + request.seasonId()));
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id = " + request.teamId()));
        Coach coach = coachRepository.findById(request.coachId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found with id = " + request.coachId()));

        assignment.setSeason(season);
        assignment.setTeam(team);
        assignment.setCoach(coach);
        assignment.setRole(request.role());
        assignment.setAssignedDate(request.assignedDate());
        assignment.setEndDate(request.endDate());
        assignment.setStatus(request.status());
    }

    private SeasonTeamCoachController.SeasonTeamCoachResponse toAssignmentResponse(SeasonTeamCoach assignment) {
        return new SeasonTeamCoachController.SeasonTeamCoachResponse(
                assignment.getId(),
                assignment.getSeason() != null ? assignment.getSeason().getId() : null,
                assignment.getSeason() != null ? assignment.getSeason().getName() : null,
                assignment.getTeam() != null ? assignment.getTeam().getId() : null,
                assignment.getTeam() != null ? assignment.getTeam().getName() : null,
                assignment.getCoach() != null ? assignment.getCoach().getId() : null,
                assignment.getCoach() != null ? assignment.getCoach().getName() : null,
                assignment.getRole(),
                assignment.getAssignedDate(),
                assignment.getEndDate(),
                assignment.getStatus()
        );
    }
}
