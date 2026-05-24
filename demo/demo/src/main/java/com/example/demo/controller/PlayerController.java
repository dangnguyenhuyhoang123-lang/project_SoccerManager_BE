package com.example.demo.controller;

import com.example.demo.dto.PlayerDTO;
import com.example.demo.dto.PlayerUpsertDTO;
import com.example.demo.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

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
    public PlayerDTO getPlayerById(@PathVariable long id)
    {
        return playerService.getPlayerById(id);
    }

    @GetMapping("/getAllPlayers")
    public Page<PlayerDTO> getAllPlayers(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String status
    ) {
        return playerService.getAllPlayers(page, size, position, status);
    }


    @GetMapping("/getPlayersByTeam/{teamID}")
    public Page<PlayerDTO> getPlayersByTeam(
            @PathVariable Long teamID,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return playerService.getPlayersByTeam(teamID,page, size);
    }


    @PostMapping("/addPlayer")
    public PlayerDTO addPlayer(@RequestBody PlayerUpsertDTO player)
    {
        return playerService.save(player);
    }

    @PutMapping("/updatePlayer/{id}")
    public PlayerDTO updatePlayer(@PathVariable Long id, @RequestBody PlayerUpsertDTO player)
    {
        return playerService.update(id,player);
    }

    @DeleteMapping("/deletePlayer/{id}")
    public void deletePlayer(@PathVariable Long id)
    {
        playerService.delete(id);
    }
}
