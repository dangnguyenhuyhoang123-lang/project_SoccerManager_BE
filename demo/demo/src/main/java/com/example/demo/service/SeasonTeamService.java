package com.example.demo.service;

import com.example.demo.controller.SeasonTeamController;
import com.example.demo.dao.registerteam.RegistrationTeamRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.entity.Season;
import com.example.demo.entity.SeasonTeam;
import com.example.demo.entity.Team;
import com.example.demo.entity.registerclub.RegistrationTeam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SeasonTeamService {

    private final SeasonTeamRepository seasonTeamRepository;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final RegistrationTeamRepository registrationTeamRepository;

    @Autowired
    public SeasonTeamService(SeasonTeamRepository seasonTeamRepository,
                             SeasonRepository seasonRepository,
                             TeamRepository teamRepository,
                             RegistrationTeamRepository registrationTeamRepository) {
        this.seasonTeamRepository = seasonTeamRepository;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
        this.registrationTeamRepository = registrationTeamRepository;
    }

    public Page<SeasonTeamController.SeasonTeamResponse> getSeasonTeams(int page, int size, Long seasonId, Long teamId) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        if (seasonId != null && teamId != null) {
            return seasonTeamRepository.findBySeasonIdAndTeamId(seasonId, teamId, pageable)
                    .map(this::toResponse);
        }
        if (seasonId != null) {
            return seasonTeamRepository.findBySeasonId(seasonId, pageable)
                    .map(this::toResponse);
        }
        if (teamId != null) {
            return seasonTeamRepository.findByTeamId(teamId, pageable)
                    .map(this::toResponse);
        }

        return seasonTeamRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public SeasonTeamController.SeasonTeamResponse getSeasonTeam(Long id) {
        return toResponse(findSeasonTeamEntity(id));
    }

    public SeasonTeamController.SeasonTeamResponse create(SeasonTeamController.SeasonTeamRequest request) {
        SeasonTeam seasonTeam = new SeasonTeam();
        applyRequest(seasonTeam, request);
        return toResponse(seasonTeamRepository.save(seasonTeam));
    }

    public SeasonTeamController.SeasonTeamResponse update(Long id, SeasonTeamController.SeasonTeamRequest request) {
        SeasonTeam seasonTeam = findSeasonTeamEntity(id);
        applyRequest(seasonTeam, request);
        return toResponse(seasonTeamRepository.save(seasonTeam));
    }

    public void delete(Long id) {
        if (!seasonTeamRepository.existsById(id)) {
            throw new ResourceNotFoundException("Season team not found with id = " + id);
        }
        seasonTeamRepository.deleteById(id);
    }

    private SeasonTeam findSeasonTeamEntity(Long id) {
        return seasonTeamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Season team not found with id = " + id));
    }

    private void applyRequest(SeasonTeam seasonTeam, SeasonTeamController.SeasonTeamRequest request) {
        Season season = seasonRepository.findById(request.seasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Season not found with id = " + request.seasonId()));
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id = " + request.teamId()));
        RegistrationTeam registrationTeam = request.registrationId() == null ? null
                : registrationTeamRepository.findById(request.registrationId())
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id = " + request.registrationId()));

        seasonTeam.setSeason(season);
        seasonTeam.setTeam(team);
        seasonTeam.setRegistrationTeam(registrationTeam);
        seasonTeam.setNotes(request.notes());
        seasonTeam.setStatus(request.status());
    }

    private SeasonTeamController.SeasonTeamResponse toResponse(SeasonTeam seasonTeam) {
        return new SeasonTeamController.SeasonTeamResponse(
                seasonTeam.getId(),
                seasonTeam.getSeason() != null ? seasonTeam.getSeason().getId() : null,
                seasonTeam.getSeason() != null ? seasonTeam.getSeason().getName() : null,
                seasonTeam.getTeam() != null ? seasonTeam.getTeam().getId() : null,
                seasonTeam.getTeam() != null ? seasonTeam.getTeam().getName() : null,
                seasonTeam.getTeam() != null ? seasonTeam.getTeam().getCity() : null,
                seasonTeam.getTeam() != null && seasonTeam.getTeam().getStadium() != null ? seasonTeam.getTeam().getStadium().getName() : null,
                seasonTeam.getRegistrationTeam() != null ? seasonTeam.getRegistrationTeam().getId() : null,
                seasonTeam.getNotes(),
                seasonTeam.getStatus()
        );
    }
}
