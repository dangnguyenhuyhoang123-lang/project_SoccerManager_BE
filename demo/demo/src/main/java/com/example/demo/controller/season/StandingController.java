package com.example.demo.controller.season;

import com.example.demo.dto.StandingResponse;
import com.example.demo.service.season.StandingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/standings")
@CrossOrigin
public class StandingController {

    private StandingService standingService;

    @Autowired
    public StandingController(StandingService standingService) {
        this.standingService = standingService;
    }

    @GetMapping("/getStandingBySeason")
    public List<StandingResponse> getStandings(@RequestParam(required = false) Long seasonId) {
        return standingService.getStandings(seasonId);
    }

    @GetMapping("/getStanding/{id}")
    public StandingResponse getStanding(@PathVariable Long id) {
        return standingService.getStanding(id);
    }



    @PostMapping("/recalculate")
    public List<StandingResponse> recalculateStandings(@RequestParam Long seasonId) {
        return standingService.recalculateBySeason(seasonId);
    }
}
