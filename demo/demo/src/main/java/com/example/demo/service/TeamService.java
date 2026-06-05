package com.example.demo.service;

import com.example.demo.controller.TeamController;
import com.example.demo.dao.StadiumRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.entity.SeasonTeam;
import com.example.demo.entity.Stadium;
import com.example.demo.entity.team.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final StadiumRepository stadiumRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final RealtimeEventService realtimeEventService;

    @Autowired
    public TeamService(
            TeamRepository teamRepository,
            StadiumRepository stadiumRepository,
            SeasonTeamRepository seasonTeamRepository,
            RealtimeEventService realtimeEventService
    ) {
        this.teamRepository = teamRepository;
        this.stadiumRepository = stadiumRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.realtimeEventService = realtimeEventService;
    }

    public Page<TeamController.TeamResponse> getTeams(int page, int size, String search, String city, Long seasonId) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        if (seasonId != null) {
            List<TeamController.TeamResponse> filtered = seasonTeamRepository.findBySeasonId(seasonId).stream()
                    .map(SeasonTeam::getTeam)
                    .filter(team -> matchesTeamFilter(team, search, city))
                    .sorted(Comparator.comparing(Team::getId))
                    .map(this::toTeamResponse)
                    .toList();
            return paginate(pageable, filtered);
        }

        Page<Team> teams;
        if (hasText(search) && hasText(city)) {
            teams = teamRepository.findByNameContainingIgnoreCaseAndCityContainingIgnoreCase(search.trim(), city.trim(), pageable);
        } else if (hasText(search)) {
            teams = teamRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        } else if (hasText(city)) {
            teams = teamRepository.findByCityContainingIgnoreCase(city.trim(), pageable);
        } else {
            teams = teamRepository.findAll(pageable);
        }

        return teams.map(this::toTeamResponse);
    }

    public TeamController.TeamResponse getTeam(Long id) {
        return toTeamResponse(findTeamEntity(id));
    }

    public TeamController.TeamResponse create(TeamController.TeamRequest request) {
        Team team = new Team();
        applyRequest(team, request);
        Team saved = teamRepository.save(team);
        sendTeamRealtimeEvent(saved.getId(), "TEAM_CREATED");
        return toTeamResponse(saved);
    }

    public TeamController.TeamResponse update(Long id, TeamController.TeamRequest request) {
        Team team = findTeamEntity(id);
        applyRequest(team, request);
        Team saved = teamRepository.save(team);
        sendTeamRealtimeEvent(saved.getId(), "TEAM_UPDATED");
        return toTeamResponse(saved);
    }

    public void delete(Long id) {
        if (!teamRepository.existsById(id)) {
            throw new ResourceNotFoundException("Team not found with id = " + id);
        }
        teamRepository.deleteById(id);
        sendTeamRealtimeEvent(id, "TEAM_DELETED");
    }

    private Team findTeamEntity(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id = " + id));
    }

    private void applyRequest(Team team, TeamController.TeamRequest request) {
        Stadium stadium = request.stadiumId() == null ? null : stadiumRepository.findById(request.stadiumId())
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found with id = " + request.stadiumId()));

        team.setName(request.name());
        team.setLogo(request.logo());
        team.setEstablishedYear(request.establishedYear());
        team.setCity(request.city());
        team.setRegion(request.region());
        team.setOwner(request.owner());
        team.setDescription(request.description());
        team.setStatus(request.status());
        team.setStadium(stadium);
    }

    private void sendTeamRealtimeEvent(Long teamId, String type) {
        RealtimeEventDTO event = realtimeEvent(
                type,
                teamId,
                "TEAM",
                "REFETCH_TEAMS"
        );

        realtimeEventService.sendToAdmins(event);
        realtimeEventService.sendToClubManagerByTeamId(teamId, event);
        realtimeEventService.sendToPublicLeagues(event);

        RealtimeEventDTO matchListEvent = realtimeEvent(
                type,
                teamId,
                "TEAM",
                "REFETCH_MATCHES"
        );

        realtimeEventService.sendToPublicMatches(matchListEvent);
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean matchesTeamFilter(Team team, String search, String city) {
        boolean matchesSearch = !hasText(search) || containsIgnoreCase(team.getName(), search);
        boolean matchesCity = !hasText(city) || containsIgnoreCase(team.getCity(), city);
        return matchesSearch && matchesCity;
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.trim().toLowerCase());
    }

    private TeamController.TeamResponse toTeamResponse(Team team) {
        return new TeamController.TeamResponse(
                team.getId(),
                team.getName(),
                team.getLogo(),
                team.getEstablishedYear(),
                team.getCity(),
                team.getRegion(),
                team.getOwner(),
                team.getDescription(),
                team.getStatus(),
                team.getStadium() != null && team.getStadium().getId() != null ? team.getStadium().getId().longValue() : null,
                team.getStadium() != null ? team.getStadium().getName() : null
        );
    }

    private <T> Page<T> paginate(Pageable pageable, List<T> items) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<T> content = start >= items.size() ? List.of() : items.subList(start, end);
        return new PageImpl<>(content, pageable, items.size());
    }
}
