package com.example.demo.service.match;

import com.example.demo.controller.match.MatchLineupController;
import com.example.demo.dao.match.MatchLineupRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.match.MatchTacticsRepository;
import com.example.demo.dao.team.player.PlayerRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dto.LineUpSubmit.MatchLineupSubmitDTO;
import com.example.demo.dto.LineUpSubmit.PlayerPositionDTO;
import com.example.demo.entity.match.Match;
import com.example.demo.entity.match.MatchLineup;
import com.example.demo.entity.match.MatchTactics;
import com.example.demo.entity.player.Player;
import com.example.demo.entity.team.Team;
import com.example.demo.service.season.SeasonTeamService;
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
    private final SeasonTeamService seasonTeamService;

    @Transactional
    public MatchLineupController.TeamLineupResponse submitLineup(MatchLineupSubmitDTO dto) {
        Match match = matchRepo.findById(dto.getMatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id = " + dto.getMatchId()));
        Team team = teamRepo.findById(dto.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id = " + dto.getTeamId()));

        validateActiveSeasonTeamForLineup(match, dto.getTeamId());

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
        Match match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id = " + matchId));
        validateActiveSeasonTeamForLineup(match, teamId);

        if (tacticsRepo.findByMatch_IdAndTeam_Id(matchId, teamId).isEmpty()) {
            throw new ResourceNotFoundException(
                    "Lineup tactics not found for match id = " + matchId + " and team id = " + teamId
            );
        }
        tacticsRepo.deleteByMatch_IdAndTeam_Id(matchId, teamId);
    }

    private void validateActiveSeasonTeamForLineup(Match match, Long teamId) {
        if (match.getSeason() == null || match.getSeason().getId() == null) {
            throw new RuntimeException("Trận đấu chưa có thông tin mùa giải hợp lệ.");
        }

        if (!isTeamInMatch(match, teamId)) {
            throw new RuntimeException("Đội bóng không thuộc trận đấu này.");
        }

        try {
            seasonTeamService.getActiveSeasonTeamOrThrow(match.getSeason().getId(), teamId);
        } catch (RuntimeException ex) {
            if (isInactiveSeasonTeamError(ex)) {
                throw new RuntimeException("Đội bóng đã bị vô hiệu hóa trong mùa giải này, không thể cập nhật đội hình.");
            }
            throw ex;
        }
    }

    private boolean isTeamInMatch(Match match, Long teamId) {
        if (teamId == null) {
            return false;
        }

        Long homeTeamId = match.getHomeTeam() != null && match.getHomeTeam().getTeam() != null
                ? match.getHomeTeam().getTeam().getId()
                : null;
        Long awayTeamId = match.getAwayTeam() != null && match.getAwayTeam().getTeam() != null
                ? match.getAwayTeam().getTeam().getId()
                : null;

        return teamId.equals(homeTeamId) || teamId.equals(awayTeamId);
    }

    private boolean isInactiveSeasonTeamError(RuntimeException ex) {
        String message = ex.getMessage();
        return message != null
                && (message.contains("vô hiệu")
                || message.contains("vÃ´")
                || message.contains("hiệu hóa")
                || message.contains("hiá»‡u hÃ³a"));
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
