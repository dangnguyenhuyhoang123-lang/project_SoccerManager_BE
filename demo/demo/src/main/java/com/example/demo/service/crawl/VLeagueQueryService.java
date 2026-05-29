package com.example.demo.service.crawl;

import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dto.crawl.VLeagueMatchResponse;
import com.example.demo.entity.Match;
import com.example.demo.entity.SeasonTeam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VLeagueQueryService {

    private final MatchRepository matchRepository;

    public VLeagueQueryService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public List<VLeagueMatchResponse> getMatches(String seasonYear) {
        return matchRepository.findVLeagueMatchesBySeasonYear(seasonYear)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private VLeagueMatchResponse toResponse(Match match) {
        VLeagueMatchResponse dto = new VLeagueMatchResponse();

        dto.setId(match.getId());
        dto.setVpfMatchCode(match.getVpfMatchCode());

        dto.setSeasonId(match.getSeason().getId());
        dto.setSeasonName(match.getSeason().getName());
        dto.setSeasonYear(match.getSeason().getYear());

        dto.setRoundNumber(match.getRound().getRoundNumber());
        dto.setRoundName(formatRoundName(match.getRound().getRoundNumber()));

        dto.setMatchDate(match.getMatchDate());
        dto.setStatus(match.getStatus());

        SeasonTeam homeSeasonTeam = match.getHomeTeam();
        SeasonTeam awaySeasonTeam = match.getAwayTeam();

        dto.setHomeSeasonTeamId(homeSeasonTeam.getId());
        dto.setHomeTeamId(homeSeasonTeam.getTeam().getId());
        dto.setHomeTeamName(homeSeasonTeam.getTeam().getName());
        dto.setHomeTeamLogo(homeSeasonTeam.getTeam().getLogo());

        dto.setAwaySeasonTeamId(awaySeasonTeam.getId());
        dto.setAwayTeamId(awaySeasonTeam.getTeam().getId());
        dto.setAwayTeamName(awaySeasonTeam.getTeam().getName());
        dto.setAwayTeamLogo(awaySeasonTeam.getTeam().getLogo());

        dto.setHomeScore(match.getHomeScore());
        dto.setAwayScore(match.getAwayScore());

        if (match.getStadium() != null) {
            dto.setStadiumId(match.getStadium().getId());
            dto.setStadiumName(match.getStadium().getName());
        }

        dto.setBroadcast(match.getBroadcast());
        dto.setAttendance(match.getAttendance());
        dto.setSourceUrl(match.getSourceUrl());

        return dto;
    }

    private String formatRoundName(Integer roundNumber) {
        if (roundNumber == null) {
            return null;
        }

        return "Vòng " + roundNumber;
    }
}
