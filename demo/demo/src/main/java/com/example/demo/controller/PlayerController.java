package com.example.demo.controller;

import com.example.demo.dto.PlayerDTO;
import com.example.demo.entity.Player;
import com.example.demo.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/player")
@CrossOrigin
public class PlayerController {
    PlayerService playerService;

    @Autowired
    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/getPlayer/{id}")
    public Player getPlayerById(@PathVariable long id)
    {
        return playerService.getPlayerById(id);
    }

    @GetMapping("/getAllPlayers")
    public Page<Player> getAllPlayers(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String status
    ) {
        return playerService.getAllPlayers(page, size, position, status);
    }

    @PostMapping("/addPlayer")
    public Player addPlayer(@RequestBody Player player)
    {
        return playerService.save(player);
    }

    @PutMapping("/updatePlayer/{id}")
    public Player updatePlayer(@PathVariable Long id, @RequestBody Player player)
    {
        return playerService.update(id,player);
    }

    @DeleteMapping("/deletePlayer/{id}")
    public void deletePlayer(@PathVariable Long id)
    {
        playerService.delete(id);
    }
}
