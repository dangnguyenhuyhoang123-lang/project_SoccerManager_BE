package com.example.demo.service;

import com.example.demo.controller.PlayerSeasonController;
import com.example.demo.dao.player.PlayerRepository;
import com.example.demo.dao.player.PlayerSeasonRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.entity.Player;
import com.example.demo.entity.PlayerSeason;
import com.example.demo.entity.Season;
import com.example.demo.entity.SeasonTeam;
import com.example.demo.entity.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerSeasonService {

    private final PlayerSeasonRepository playerSeasonRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonTeamRepository seasonTeamRepository;

    @Autowired
    public PlayerSeasonService(PlayerSeasonRepository playerSeasonRepository,
                               PlayerRepository playerRepository,
                               TeamRepository teamRepository,
                               SeasonRepository seasonRepository,
                               SeasonTeamRepository seasonTeamRepository) {
        this.playerSeasonRepository = playerSeasonRepository;
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.seasonRepository = seasonRepository;
        this.seasonTeamRepository = seasonTeamRepository;
    }

    public Page<PlayerSeasonController.PlayerSeasonResponse> getPlayerSeasons(
            int page,
            int size,
            Long seasonId,
            Long teamId,
            Long playerId
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        if (seasonId != null && teamId != null) {
            return playerSeasonRepository.findBySeasonIdAndTeamId(seasonId, teamId, pageable)
                    .map(this::toResponse);
        }
        if (seasonId != null) {
            return playerSeasonRepository.findBySeasonId(seasonId, pageable)
                    .map(this::toResponse);
        }
        if (teamId != null) {
            return playerSeasonRepository.findByTeamId(teamId, pageable)
                    .map(this::toResponse);
        }
        if (playerId != null) {
            return playerSeasonRepository.findByPlayerId(playerId, pageable)
                    .map(this::toResponse);
        }

        return playerSeasonRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public PlayerSeasonController.PlayerSeasonResponse getPlayerSeason(Long id) {
        return toResponse(findPlayerSeasonEntity(id));
    }

    public List<PlayerSeasonController.PlayerSeasonResponse> getPlayerSeasonsByTeamId(Long teamId) {
        return playerSeasonRepository.findByTeamIdOrderByShirtNumberAsc(teamId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PlayerSeasonController.PlayerSeasonResponse> getPlayerSeasonsByTeamSeasonId(Long teamSeasonId) {
        return playerSeasonRepository.findByTeamSeasonIdOrderByShirtNumberAsc(teamSeasonId).stream()
                .map(this::toResponse)
                .toList();
    }

    public PlayerSeasonController.PlayerSeasonResponse create(PlayerSeasonController.PlayerSeasonRequest request) {
        PlayerSeason playerSeason = new PlayerSeason();
        applyRequest(playerSeason, request);
        return toResponse(playerSeasonRepository.save(playerSeason));
    }

    public PlayerSeasonController.PlayerSeasonResponse update(Long id, PlayerSeasonController.PlayerSeasonRequest request) {
        PlayerSeason playerSeason = findPlayerSeasonEntity(id);
        applyRequest(playerSeason, request);
        return toResponse(playerSeasonRepository.save(playerSeason));
    }

    public void delete(Long id) {
        if (!playerSeasonRepository.existsById(id)) {
            throw new ResourceNotFoundException("Player season not found with id = " + id);
        }
        playerSeasonRepository.deleteById(id);
    }

    private PlayerSeason findPlayerSeasonEntity(Long id) {
        return playerSeasonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player season not found with id = " + id));
    }

    private void applyRequest(PlayerSeason playerSeason, PlayerSeasonController.PlayerSeasonRequest request) {
        Player player = playerRepository.findById(request.playerId())
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id = " + request.playerId()));
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id = " + request.teamId()));
        Season season = seasonRepository.findById(request.seasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Season not found with id = " + request.seasonId()));
        SeasonTeam teamSeason = seasonTeamRepository.findById(request.teamSeasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Season team not found with id = " + request.teamSeasonId()));

        playerSeason.setPlayer(player);
        playerSeason.setTeam(team);
        playerSeason.setSeason(season);
        playerSeason.setTeamSeason(teamSeason);
        playerSeason.setShirtNumber(request.shirtNumber());
        playerSeason.setPrimaryPosition(request.primaryPosition());
        playerSeason.setContractStart(request.contractStart());
        playerSeason.setContractEnd(request.contractEnd());
    }

    private PlayerSeasonController.PlayerSeasonResponse toResponse(PlayerSeason playerSeason) {
        return new PlayerSeasonController.PlayerSeasonResponse(
                playerSeason.getId(),
                playerSeason.getPlayer() != null ? playerSeason.getPlayer().getId() : null,
                playerSeason.getPlayer() != null ? playerSeason.getPlayer().getName() : null,
                playerSeason.getTeam() != null ? playerSeason.getTeam().getId() : null,
                playerSeason.getTeam() != null ? playerSeason.getTeam().getName() : null,
                playerSeason.getSeason() != null ? playerSeason.getSeason().getId() : null,
                playerSeason.getSeason() != null ? playerSeason.getSeason().getName() : null,
                playerSeason.getTeamSeason() != null ? playerSeason.getTeamSeason().getId() : null,
                playerSeason.getShirtNumber(),
                playerSeason.getPrimaryPosition(),
                playerSeason.getContractStart(),
                playerSeason.getContractEnd()
        );
    }
}
