package com.example.demo.controller;

import com.example.demo.dto.matchevent.MatchEventResponse;
import com.example.demo.dto.matchevent.MatchEventUpsertRequest;
import com.example.demo.service.MatchEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchEventController {

    private final MatchEventService matchEventService;

    @GetMapping("/{matchId}/events")
    public List<MatchEventResponse> getEvents(@PathVariable Long matchId) {
        return matchEventService.getEventsByMatch(matchId);
    }

    @PostMapping("/{matchId}/events")
    public MatchEventResponse createEvent(
            @PathVariable Long matchId,
            @RequestBody MatchEventUpsertRequest request
    ) {
        return matchEventService.createEvent(matchId, request);
    }

    @PutMapping("/{matchId}/events/{eventId}")
    public MatchEventResponse updateEvent(
            @PathVariable Long matchId,
            @PathVariable Long eventId,
            @RequestBody MatchEventUpsertRequest request
    ) {
        return matchEventService.updateEvent(matchId, eventId, request);
    }

    @DeleteMapping("/{matchId}/events/{eventId}")
    public void deleteEvent(
            @PathVariable Long matchId,
            @PathVariable Long eventId
    ) {
        matchEventService.deleteEvent(matchId, eventId);
    }


}