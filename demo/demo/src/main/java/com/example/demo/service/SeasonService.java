package com.example.demo.service;

import com.example.demo.controller.SeasonController;
import com.example.demo.dao.LeagueRepository;
import com.example.demo.dao.SystemRuleRepo;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.entity.League;
import com.example.demo.entity.Season;
import com.example.demo.entity.SeasonTeam;
import com.example.demo.entity.Stadium;
import com.example.demo.entity.SystemRule;
import com.example.demo.entity.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final LeagueRepository leagueRepository;
    private final SystemRuleRepo systemRuleRepo;
    private final SeasonTeamRepository seasonTeamRepository;

    @Autowired
    public SeasonService(SeasonRepository seasonRepository,
                         LeagueRepository leagueRepository,
                         SystemRuleRepo systemRuleRepo,
                         SeasonTeamRepository seasonTeamRepository) {
        this.seasonRepository = seasonRepository;
        this.leagueRepository = leagueRepository;
        this.systemRuleRepo = systemRuleRepo;
        this.seasonTeamRepository = seasonTeamRepository;
    }

    public Page<SeasonController.SeasonResponse> getSeasons(int page, int size, Long leagueId) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        if (leagueId != null) {
            List<SeasonController.SeasonResponse> content = seasonRepository.findByLeagueId(leagueId).stream()
                    .map(this::toSeasonResponse)
                    .toList();
            return paginate(pageable, content);
        }
        return seasonRepository.findAll(pageable).map(this::toSeasonResponse);
    }

    public SeasonController.SeasonResponse getSeason(Long id) {
        return toSeasonResponse(findSeasonEntity(id));
    }

    public List<SeasonController.SeasonTeamResponse> getSeasonTeams(Long seasonId) {
        if (!seasonRepository.existsById(seasonId)) {
            throw new ResourceNotFoundException("Season not found with id = " + seasonId);
        }
        return seasonTeamRepository.findBySeasonId(seasonId).stream()
                .map(this::toSeasonTeamResponse)
                .toList();
    }

    public SeasonController.SeasonResponse create(SeasonController.SeasonRequest request) {
        Season season = new Season();
        applySeasonRequest(season, request);
        return toSeasonResponse(seasonRepository.save(season));
    }

    public SeasonController.SeasonResponse update(Long id, SeasonController.SeasonRequest request) {
        Season season = findSeasonEntity(id);
        applySeasonRequest(season, request);
        return toSeasonResponse(seasonRepository.save(season));
    }

    public void delete(Long id) {
        if (!seasonRepository.existsById(id)) {
            throw new ResourceNotFoundException("Season not found with id = " + id);
        }
        seasonRepository.deleteById(id);
    }

    private Season findSeasonEntity(Long id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found with id = " + id));
    }

    private void applySeasonRequest(Season season, SeasonController.SeasonRequest request) {
        League league = request.leagueId() == null ? null : leagueRepository.findById(request.leagueId())
                .orElseThrow(() -> new ResourceNotFoundException("League not found with id = " + request.leagueId()));
        SystemRule systemRule = request.systemRuleId() == null ? null : systemRuleRepo.findById(request.systemRuleId())
                .orElseThrow(() -> new ResourceNotFoundException("System rule not found with id = " + request.systemRuleId()));

        season.setYear(request.year());
        season.setName(request.name());
        season.setStartDate(request.startDate());
        season.setEndDate(request.endDate());
        season.setLeague(league);
        season.setSystemRule(systemRule);
    }

    private SeasonController.SeasonResponse toSeasonResponse(Season season) {
        return new SeasonController.SeasonResponse(
                season.getId(),
                season.getYear(),
                season.getName(),
                season.getStartDate(),
                season.getEndDate(),
                season.getLeague() != null ? season.getLeague().getId() : null,
                season.getLeague() != null ? season.getLeague().getName() : null,
                season.getSystemRule() != null ? season.getSystemRule().getId() : null
        );
    }

    private SeasonController.SeasonTeamResponse toSeasonTeamResponse(SeasonTeam seasonTeam) {
        Team team = seasonTeam.getTeam();
        Stadium stadium = team != null ? team.getStadium() : null;
        return new SeasonController.SeasonTeamResponse(
                seasonTeam.getId(),
                team != null ? team.getId() : null,
                team != null ? team.getName() : null,
                team != null ? team.getCity() : null,
                stadium != null ? stadium.getName() : null,
                seasonTeam.getStatus()
        );
    }

    private <T> Page<T> paginate(Pageable pageable, List<T> items) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<T> content = start >= items.size() ? List.of() : items.subList(start, end);
        return new PageImpl<>(content, pageable, items.size());
    }
}
