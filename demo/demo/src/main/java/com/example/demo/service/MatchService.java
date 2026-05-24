package com.example.demo.service;

import com.example.demo.dao.RoundRepository;
import com.example.demo.dao.StadiumRepository;
import com.example.demo.dao.match.MatchEventRepository;
import com.example.demo.dao.match.MatchLineupRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.match.MatchStatsRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dto.LeagueDTO;
import com.example.demo.dto.MatchDTO;
import com.example.demo.dto.MatchUpsertDTO;
import com.example.demo.dto.RoundDTO;
import com.example.demo.dto.SeasonDTO;
import com.example.demo.dto.StadiumDTO;
import com.example.demo.dto.TeamDTO;
import com.example.demo.entity.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchStatsRepository matchStatsRepository;

    @Autowired
    private MatchEventRepository matchEventRepository;

    @Autowired
    private MatchLineupRepository matchLineupRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private StadiumRepository stadiumRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private SeasonTeamRepository seasonTeamRepository;

    public MatchDTO getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        return toDTO(match);
    }

    public Page<MatchDTO> getAllMatches(int page, int size, String status, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("matchDate").descending());

        boolean noStatus = (status == null || status.isBlank());
        boolean noSearch = (search == null || search.isBlank());

        if (noStatus && noSearch) {
            return matchRepository.findAll(pageable)
                    .map(this::toDTO);
        }

        MatchStatus finalStatus = noStatus ? null : MatchStatus.valueOf(status.trim().toUpperCase());
        String finalSearch = noSearch ? "" : search;

        return matchRepository.filterMatches(finalStatus, finalSearch, pageable)
                .map(this::toDTO);
    }

    public MatchDTO save(MatchUpsertDTO request) {
        Match match = new Match();
        applyRequest(match, request);

        if (match.getStadium() == null && match.getHomeTeam() != null) {
            match.setStadium(match.getHomeTeam().getTeam().getStadium());
        }

        if (match.getStatus() == null) {
            match.setStatus(MatchStatus.SCHEDULED);
        }

        if (match.getHomeScore() == null) {
            match.setHomeScore(-1);
        }
        if (match.getAwayScore() == null) {
            match.setAwayScore(-1);
        }

        return toDTO(matchRepository.save(match));
    }

    @Transactional
    public MatchDTO update(Long id, MatchUpsertDTO request) {
        Match existing = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        applyRequest(existing, request);
        return toDTO(matchRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!matchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Match not found with id = " + id);
        }
        matchRepository.deleteById(id);
    }

    private void applyRequest(Match match, MatchUpsertDTO request) {
        SeasonTeam homeTeam = seasonTeamRepository.findById(request.getHomeTeamId())
                .orElseThrow(() -> new RuntimeException("Home team not found with id = " + request.getHomeTeamId()));
        SeasonTeam awayTeam = seasonTeamRepository.findById(request.getAwayTeamId())
                .orElseThrow(() -> new RuntimeException("Away team not found with id = " + request.getAwayTeamId()));
        Season season = seasonRepository.findById(request.getSeasonId())
                .orElseThrow(() -> new RuntimeException("Season not found with id = " + request.getSeasonId()));
        Round round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new RuntimeException("Round not found with id = " + request.getRoundId()));

        match.setStatus(request.getStatus());
        match.setHomeScore(request.getHomeScore());
        match.setAwayScore(request.getAwayScore());
        match.setMatchDate(request.getMatchDate());
        match.setSeason(season);
        match.setRound(round);
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);

        if (request.getStadiumId() != null) {
            Stadium stadium = stadiumRepository.findById(request.getStadiumId())
                    .orElseThrow(() -> new RuntimeException("Stadium not found with id = " + request.getStadiumId()));
            match.setStadium(stadium);
        } else {
            match.setStadium(null);
        }
    }

    private MatchDTO toDTO(Match match) {
        return new MatchDTO(
                match.getId(),
                match.getStatus() != null ? match.getStatus().name() : null,
                match.getHomeScore(),
                match.getAwayScore(),
                match.getMatchDate(),
                toStadiumDto(match.getStadium()),
                toRoundDto(match.getRound()),
                toTeamDto(match.getHomeTeam().getTeam()),
                toTeamDto(match.getAwayTeam().getTeam()),
                toSeasonDto(match.getSeason())
        );
    }

    private TeamDTO toTeamDto(Team team) {
        if (team == null) {
            return null;
        }

        return new TeamDTO(
                team.getId(),
                team.getName(),
                team.getLogo(),
                team.getCity(),
                team.getStatus(),
                team.getStadium() != null ? Long.valueOf(team.getStadium().getId()) : null,
                team.getStadium() != null ? team.getStadium().getName() : null
        );
    }

    private SeasonDTO toSeasonDto(Season season) {
        if (season == null) {
            return null;
        }

        LeagueDTO leagueDto = null;
        if (season.getLeague() != null) {
            leagueDto = new LeagueDTO(
                    season.getLeague().getId(),
                    season.getLeague().getName(),
                    season.getLeague().getCountry(),
                    season.getLeague().getScale(),
                    season.getLeague().getStatus()
            );
        }

        return new SeasonDTO(
                season.getId(),
                season.getYear(),
                season.getName(),
                season.getStartDate(),
                season.getEndDate(),
                leagueDto
        );
    }

    private StadiumDTO toStadiumDto(Stadium stadium) {
        if (stadium == null) {
            return null;
        }

        return new StadiumDTO(
                Long.valueOf(stadium.getId()),
                stadium.getName(),
                stadium.getAddress(),
                stadium.getCapacity(),
                stadium.getGrass()
        );
    }

    private RoundDTO toRoundDto(Round round) {
        if (round == null) {
            return null;
        }

        return new RoundDTO(
                round.getId(),
                round.getRoundNumber(),
                round.getName(),
                round.getStartDate(),
                round.getEndDate(),
                round.getStatus(),
                round.getSeason() != null ? round.getSeason().getId() : null
        );
    }
}
