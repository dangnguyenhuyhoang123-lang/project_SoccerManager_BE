package com.example.demo.controller.crawl;

import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dto.crawl.VLeagueMatchResponse;
import com.example.demo.entity.Match;
import com.example.demo.entity.Season;
import com.example.demo.service.crawl.VLeagueQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vleague")
public class VLeagueController {

    private final VLeagueQueryService vLeagueQueryService;

    public VLeagueController(VLeagueQueryService vLeagueQueryService) {
        this.vLeagueQueryService = vLeagueQueryService;
    }

    @GetMapping("/matches")
    public List<VLeagueMatchResponse> getMatches(
            @RequestParam(defaultValue = "2025-2026") String seasonYear
    ) {
        return vLeagueQueryService.getMatches(seasonYear);
    }
}
