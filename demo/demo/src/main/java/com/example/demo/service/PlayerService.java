package com.example.demo.service;


import com.example.demo.dao.player.PlayerRepository;
import com.example.demo.entity.Player;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

    private PlayerRepository playerRepository;

    @Autowired
    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public Player getPlayerById(long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found with id = " + playerId));
    }

    public Page<Player> getAllPlayers(int page, int size, String position, String status) {
        Pageable pageable = PageRequest.of(page, size);

        if (position != null && status != null) {
            return playerRepository.findByPositionAndStatus(position, status, pageable);
        } else if (position != null) {
            return playerRepository.findByPosition(position, pageable);
        } else if (status != null) {
            return playerRepository.findByStatus(status, pageable);
        }

        return playerRepository.findAll(pageable);
    }

    public Player save(Player player)
    {
        return playerRepository.save(player);
    }

    @Transactional
    public Player update(Long id,Player player)
    {
        Player existing = getPlayerById(id);

        existing.setName(player.getName());
        existing.setAvatar(player.getAvatar());
        existing.setShirtNumber(player.getShirtNumber());
        existing.setStatus(player.getStatus());
        existing.setPosition(player.getPosition());

        return playerRepository.save(existing);
    }

    @Transactional
    public void delete(Long id)
    {
        if (!playerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Player not found with id = " + id);
        }
        playerRepository.deleteById(id);
    }
}
