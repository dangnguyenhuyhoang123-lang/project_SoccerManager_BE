package com.example.demo.service.team.player;

import com.example.demo.dao.team.player.PlayerRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dto.PlayerDTO;
import com.example.demo.dto.PlayerSearchResponse;
import com.example.demo.dto.PlayerUpsertDTO;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.entity.player.Player;
import com.example.demo.entity.team.Team;
import com.example.demo.service.realtime.RealtimeEventService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final RealtimeEventService realtimeEventService;

    @Autowired
    public PlayerService(
            PlayerRepository playerRepository,
            TeamRepository teamRepository,
            RealtimeEventService realtimeEventService
    ) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.realtimeEventService = realtimeEventService;
    }

    public PlayerDTO getPlayerById(long playerId) {
        return toDto(findPlayerEntity(playerId));
    }

    public Page<PlayerDTO> getAllPlayers(int page, int size, String position, String status) {
        Pageable pageable = PageRequest.of(page, size);

        if (position != null && status != null) {
            return playerRepository.findByPositionAndStatus(position, status, pageable)
                    .map(this::toDto);
        } else if (position != null) {
            return playerRepository.findByPosition(position, pageable)
                    .map(this::toDto);
        } else if (status != null) {
            return playerRepository.findByStatus(status, pageable)
                    .map(this::toDto);
        }

        return playerRepository.findAll(pageable)
                .map(this::toDto);
    }

    public Page<PlayerDTO> getPlayersByTeam(Long teamID,int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return playerRepository.findByTeamId(teamID,pageable)
                .map(this::toDto);
    }
    public List<PlayerSearchResponse> searchPlayers(
            Long seasonId,
            Long teamId,
            String keyword,
            String playerType
    ) {
        String normalizedKeyword =
                keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase();

        List<PlayerSearchResponse> result =
                playerRepository.searchPlayersFromPlayer(seasonId, teamId, normalizedKeyword);

        if (playerType == null || playerType.isBlank()) {
            return result;
        }

        return result.stream()
                .filter(p -> playerType.equalsIgnoreCase(p.getPlayerType()))
                .toList();
    }

    public PlayerDTO save(PlayerUpsertDTO request)
    {
        Player player = new Player();
        applyRequest(player, request);
        Player saved = playerRepository.save(player);
        sendPlayerRealtimeEvent(saved.getId(), saved.getTeam() != null ? saved.getTeam().getId() : null, "PLAYER_CREATED");
        return toDto(saved);
    }

    @Transactional
    public PlayerDTO update(Long id, PlayerUpsertDTO request)
    {
        Player existing = findPlayerEntity(id);
        applyRequest(existing, request);
        Player saved = playerRepository.save(existing);
        sendPlayerRealtimeEvent(saved.getId(), saved.getTeam() != null ? saved.getTeam().getId() : null, "PLAYER_UPDATED");
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id)
    {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id = " + id));
        Long teamId = player.getTeam() != null ? player.getTeam().getId() : null;
        playerRepository.deleteById(id);
        sendPlayerRealtimeEvent(id, teamId, "PLAYER_DELETED");
    }

    private Player findPlayerEntity(long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found with id = " + playerId));
    }

    private void applyRequest(Player player, PlayerUpsertDTO request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("Team not found with id = " + request.getTeamId()));

        player.setName(request.getName());
        player.setIDCode(request.getIdCode());
        player.setAvatar(request.getAvatar());
        player.setDateOfBirth(request.getDateOfBirth());
        player.setPosition(request.getPosition());
        player.setDetailPosition(request.getDetailPosition());
        player.setShirtNumber(request.getShirtNumber());
        player.setNationality(request.getNationality());
        player.setHeight(request.getHeight());
        player.setWeight(request.getWeight());
        player.setStatus(request.getStatus());
        player.setTeam(team);
    }

    private void sendPlayerRealtimeEvent(Long playerId, Long teamId, String type) {
        RealtimeEventDTO event = realtimeEvent(
                type,
                playerId,
                "PLAYER",
                "REFETCH_PLAYERS"
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

    private PlayerDTO toDto(Player player) {
        return new PlayerDTO(
                player.getId(),
                player.getName(),
                player.getIDCode(),
                player.getDateOfBirth(),
                player.getPosition(),
                player.getDetailPosition(),
                player.getShirtNumber(),
                player.getNationality(),
                player.getHeight(),
                player.getWeight(),
                player.getStatus(),
                player.getAvatar(),
                player.getTeam() != null ? player.getTeam().getId() : null,
                player.getTeam() != null ? player.getTeam().getName() : null
        );
    }
}
