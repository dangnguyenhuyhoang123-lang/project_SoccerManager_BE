package com.example.demo.controller.match;

import com.example.demo.dto.matchreferee.MatchRefereeAssignRequest;
import com.example.demo.dto.matchreferee.MatchRefereeResponse;
import com.example.demo.service.match.MatchRefereeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/match-referees")
@RequiredArgsConstructor
@CrossOrigin
public class MatchRefereeController {

    private final MatchRefereeService matchRefereeService;

    @PostMapping
    public MatchRefereeResponse assign(@RequestBody MatchRefereeAssignRequest request) {
        return matchRefereeService.assign(request);
    }

    @GetMapping("/match/{matchId}")
    public List<MatchRefereeResponse> getByMatch(@PathVariable Long matchId) {
        return matchRefereeService.getByMatch(matchId);
    }

    @DeleteMapping("/{id}")
    public void remove(@PathVariable Long id) {
        matchRefereeService.remove(id);
    }
}
