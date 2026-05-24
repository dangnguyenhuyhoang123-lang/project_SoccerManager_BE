package com.example.demo.service;

import com.example.demo.dao.player.PlayerRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dto.PlayerDTO;
import com.example.demo.dto.PlayerUpsertDTO;
import com.example.demo.entity.Player;
import com.example.demo.entity.Team;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    @Autowired
    public PlayerService(PlayerRepository playerRepository, TeamRepository teamRepository) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
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

    public PlayerDTO save(PlayerUpsertDTO request)
    {
        Player player = new Player();
        applyRequest(player, request);
        return toDto(playerRepository.save(player));
    }

    @Transactional
    public PlayerDTO update(Long id, PlayerUpsertDTO request)
    {
        Player existing = findPlayerEntity(id);
        applyRequest(existing, request);
        return toDto(playerRepository.save(existing));
    }

    @Transactional
    public void delete(Long id)
    {
        if (!playerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Player not found with id = " + id);
        }
        playerRepository.deleteById(id);
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
