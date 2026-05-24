package com.example.demo.service;

import com.example.demo.controller.MatchLineupController;
import com.example.demo.dao.match.MatchLineupRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.match.MatchTacticsRepository;
import com.example.demo.dao.player.PlayerRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dto.LineUpSubmit.MatchLineupSubmitDTO;
import com.example.demo.dto.LineUpSubmit.PlayerPositionDTO;
import com.example.demo.entity.Match;
import com.example.demo.entity.MatchLineup;
import com.example.demo.entity.MatchTactics;
import com.example.demo.entity.Player;
import com.example.demo.entity.Team;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchLineupService {

    private final MatchTacticsRepository tacticsRepo;
    private final MatchLineupRepository lineupRepo;
    private final MatchRepository matchRepo;
    private final TeamRepository teamRepo;
    private final PlayerRepository playerRepo;

    @Transactional
    public MatchLineupController.TeamLineupResponse submitLineup(MatchLineupSubmitDTO dto) {
        Match match = matchRepo.findById(dto.getMatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id = " + dto.getMatchId()));
        Team team = teamRepo.findById(dto.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id = " + dto.getTeamId()));

        tacticsRepo.deleteByMatch_IdAndTeam_Id(dto.getMatchId(), dto.getTeamId());

        MatchTactics tactics = new MatchTactics();
        tactics.setMatch(match);
        tactics.setTeam(team);
        tactics.setFormationName(dto.getFormationName());
        tactics.setDescription(dto.getDescription());
        MatchTactics savedTactics = tacticsRepo.save(tactics);

        List<MatchLineup> lineups = dto.getPlayers() == null ? List.of() : dto.getPlayers().stream()
                .map(playerDto -> toMatchLineup(savedTactics, playerDto))
                .collect(Collectors.toList());

        lineupRepo.saveAll(lineups);

        return getLineupByMatchAndTeam(dto.getMatchId(), dto.getTeamId());
    }

    public List<MatchLineupController.TeamLineupResponse> getLineupsByMatch(Long matchId) {
        List<MatchLineup> lineups = lineupRepo.findByMatchTactics_Match_IdOrderByMatchTactics_IdAscLineupOrderAsc(matchId);
        Map<Long, List<MatchLineup>> grouped = lineups.stream()
                .collect(Collectors.groupingBy(lineup -> lineup.getMatchTactics().getId(), LinkedHashMap::new, Collectors.toList()));

        return grouped.values().stream()
                .map(this::toTeamLineupResponse)
                .toList();
    }

    public MatchLineupController.TeamLineupResponse getLineupByMatchAndTeam(Long matchId, Long teamId) {
        MatchTactics tactics = tacticsRepo.findByMatch_IdAndTeam_Id(matchId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lineup tactics not found for match id = " + matchId + " and team id = " + teamId
                ));

        List<MatchLineup> lineups = lineupRepo.findByMatchTactics_Match_IdAndMatchTactics_Team_IdOrderByLineupOrderAsc(matchId, teamId);
        return toTeamLineupResponse(tactics, lineups);
    }

    public MatchLineupController.TeamLineupResponse getLineupByTactics(Long tacticsId) {
        MatchTactics tactics = tacticsRepo.findOneById(tacticsId)
                .orElseThrow(() -> new ResourceNotFoundException("Tactics not found with id = " + tacticsId));

        List<MatchLineup> lineups = lineupRepo.findByMatchTactics_IdOrderByLineupOrderAsc(tacticsId);
        return toTeamLineupResponse(tactics, lineups);
    }

    @Transactional
    public void deleteLineup(Long matchId, Long teamId) {
        if (tacticsRepo.findByMatch_IdAndTeam_Id(matchId, teamId).isEmpty()) {
            throw new ResourceNotFoundException(
                    "Lineup tactics not found for match id = " + matchId + " and team id = " + teamId
            );
        }
        tacticsRepo.deleteByMatch_IdAndTeam_Id(matchId, teamId);
    }

    private MatchLineup toMatchLineup(MatchTactics tactics, PlayerPositionDTO playerDto) {
        Player player = playerRepo.findById(playerDto.getPlayerId())
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id = " + playerDto.getPlayerId()));

        MatchLineup lineup = new MatchLineup();
        lineup.setMatchTactics(tactics);
        lineup.setPlayer(player);
        lineup.setRole(playerDto.getRole());
        lineup.setPosition(playerDto.getPosition());
        lineup.setIsStarting(playerDto.getIsStarting());
        lineup.setLineupOrder(playerDto.getLineupOrder());
        lineup.setShirtNumber(playerDto.getShirtNumber() != null ? playerDto.getShirtNumber() : player.getShirtNumber());
        return lineup;
    }

    private MatchLineupController.TeamLineupResponse toTeamLineupResponse(List<MatchLineup> lineups) {
        if (lineups == null || lineups.isEmpty()) {
            throw new ResourceNotFoundException("Lineup not found");
        }
        MatchTactics tactics = lineups.get(0).getMatchTactics();
        return toTeamLineupResponse(tactics, lineups);
    }

    private MatchLineupController.TeamLineupResponse toTeamLineupResponse(MatchTactics tactics, List<MatchLineup> lineups) {
        List<MatchLineupController.LineupPlayerResponse> players = lineups.stream()
                .map(this::toLineupPlayerResponse)
                .toList();

        return new MatchLineupController.TeamLineupResponse(
                tactics.getId(),
                tactics.getMatch() != null ? tactics.getMatch().getId() : null,
                tactics.getTeam() != null ? tactics.getTeam().getId() : null,
                tactics.getTeam() != null ? tactics.getTeam().getName() : null,
                tactics.getFormationName(),
                tactics.getDescription(),
                players
        );
    }

    private MatchLineupController.LineupPlayerResponse toLineupPlayerResponse(MatchLineup lineup) {
        return new MatchLineupController.LineupPlayerResponse(
                lineup.getId(),
                lineup.getPlayer() != null ? lineup.getPlayer().getId() : null,
                lineup.getPlayer() != null ? lineup.getPlayer().getName() : null,
                lineup.getPlayer() != null ? lineup.getPlayer().getAvatar() : null,
                lineup.getShirtNumber(),
                lineup.getPosition(),
                lineup.getIsStarting(),
                lineup.getLineupOrder(),
                lineup.getRole()
        );
    }
}
