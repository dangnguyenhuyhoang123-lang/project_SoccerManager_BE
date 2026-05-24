package com.example.demo.controller;

import com.example.demo.service.CoachService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/coaches")
@CrossOrigin
public class CoachController {

    private final CoachService coachService;

    @Autowired
    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    @GetMapping("/getCoaches")
    public Page<CoachResponse> getCoaches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return coachService.getCoaches(page, size, search, status);
    }

    @GetMapping("/getCoachesByTeam/{teamId}")
    public Page<CoachResponse> getCoachesByTeamId(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size

    ) {
        return coachService.getCoachesByTeamId(teamId,page, size);
    }

    @GetMapping("/getCoach/{id}")
    public CoachResponse getCoach(@PathVariable Long id) {
        return coachService.getCoach(id);
    }

    @PostMapping("/addCoach")
    public CoachResponse createCoach(@RequestBody CoachRequest request) {
        return coachService.create(request);
    }

    @PutMapping("/updateCoach/{id}")
    public CoachResponse updateCoach(@PathVariable Long id, @RequestBody CoachRequest request) {
        return coachService.update(id, request);
    }

    @DeleteMapping("/deleteCoach/{id}")
    public void deleteCoach(@PathVariable Long id) {
        coachService.delete(id);
    }

    public record CoachRequest(
            String name,
            String nationality,
            String idCode,
            String avatar,
            LocalDate birthDay,
            String description,
            String status,
            Long teamId
    ) {
    }

    public record CoachResponse(
            Long id,
            String name,
            String nationality,
            String idCode,
            String avatar,
            LocalDate birthDay,
            String description,
            String status,
            Long teamId,
            String teamName
    ) {
    }
}
