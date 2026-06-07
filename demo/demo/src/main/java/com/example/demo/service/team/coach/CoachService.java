package com.example.demo.service.team.coach;

import com.example.demo.controller.team.CoachController;
import com.example.demo.dao.team.CoachRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.entity.team.Coach;
import com.example.demo.entity.team.Team;
import com.example.demo.service.realtime.RealtimeEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CoachService {

    private final CoachRepository coachRepository;
    private final TeamRepository teamRepository;
    private final RealtimeEventService realtimeEventService;

    @Autowired
    public CoachService(
            CoachRepository coachRepository,
            TeamRepository teamRepository,
            RealtimeEventService realtimeEventService
    ) {
        this.coachRepository = coachRepository;
        this.teamRepository = teamRepository;
        this.realtimeEventService = realtimeEventService;
    }

    public Page<CoachController.CoachResponse> getCoaches(int page, int size, String search, String status) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        if (hasText(search) && hasText(status)) {
            return coachRepository.findByNameContainingIgnoreCaseAndStatus(search.trim(), status.trim(), pageable)
                    .map(this::toCoachResponse);
        }
        if (hasText(search)) {
            return coachRepository.findByNameContainingIgnoreCase(search.trim(), pageable)
                    .map(this::toCoachResponse);
        }
        if (hasText(status)) {
            return coachRepository.findByStatus(status.trim(), pageable)
                    .map(this::toCoachResponse);
        }

        return coachRepository.findAll(pageable)
                .map(this::toCoachResponse);
    }
    public Page<CoachController.CoachResponse> getCoachesByTeamId(Long teamId,int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));



        return coachRepository.findByTeamId(teamId,pageable)
                .map(this::toCoachResponse);
    }

    public CoachController.CoachResponse getCoach(Long id) {
        return toCoachResponse(findCoachEntity(id));
    }

    public CoachController.CoachResponse create(CoachController.CoachRequest request) {
        Coach coach = new Coach();
        applyRequest(coach, request);
        Coach saved = coachRepository.save(coach);
        sendCoachRealtimeEvent(saved.getId(), saved.getTeam() != null ? saved.getTeam().getId() : null, "COACH_CREATED");
        return toCoachResponse(saved);
    }

    public CoachController.CoachResponse update(Long id, CoachController.CoachRequest request) {
        Coach coach = findCoachEntity(id);
        applyRequest(coach, request);
        Coach saved = coachRepository.save(coach);
        sendCoachRealtimeEvent(saved.getId(), saved.getTeam() != null ? saved.getTeam().getId() : null, "COACH_UPDATED");
        return toCoachResponse(saved);
    }

    public void delete(Long id) {
        Coach coach = findCoachEntity(id);
        Long teamId = coach.getTeam() != null ? coach.getTeam().getId() : null;
        coachRepository.deleteById(id);
        sendCoachRealtimeEvent(id, teamId, "COACH_DELETED");
    }

    private Coach findCoachEntity(Long id) {
        return coachRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found with id = " + id));
    }

    private void applyRequest(Coach coach, CoachController.CoachRequest request) {
        Team team = request.teamId() == null ? null : teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id = " + request.teamId()));

        coach.setName(request.name());
        coach.setNationality(request.nationality());
        coach.setIDCode(request.idCode());
        coach.setAvatar(request.avatar());
        coach.setBirthDay(request.birthDay());
        coach.setDes(request.description());
        coach.setStatus(request.status());
        coach.setTeam(team);
    }

    private void sendCoachRealtimeEvent(Long coachId, Long teamId, String type) {
        RealtimeEventDTO event = realtimeEvent(
                type,
                coachId,
                "COACH",
                "REFETCH_COACHES"
        );

        realtimeEventService.sendToAdmins(event);
        realtimeEventService.sendToClubManagerByTeamId(teamId, event);
    }

    private RealtimeEventDTO realtimeEvent(
            String type,
            Long referenceId,
            String referenceType,
            String action
    ) {
        return new RealtimeEventDTO(
                type,
                referenceId,
                referenceType,
                action,
                null,
                LocalDateTime.now()
        );
    }

    private CoachController.CoachResponse toCoachResponse(Coach coach) {
        return new CoachController.CoachResponse(
                coach.getId(),
                coach.getName(),
                coach.getNationality(),
                coach.getIDCode(),
                coach.getAvatar(),
                coach.getBirthDay(),
                coach.getDes(),
                coach.getStatus(),
                coach.getTeam() != null ? coach.getTeam().getId() : null,
                coach.getTeam() != null ? coach.getTeam().getName() : null
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
