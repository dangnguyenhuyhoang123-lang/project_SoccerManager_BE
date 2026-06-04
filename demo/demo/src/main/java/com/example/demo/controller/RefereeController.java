package com.example.demo.controller;

import com.example.demo.dto.referee.RefereeRequest;
import com.example.demo.dto.referee.RefereeResponse;
import com.example.demo.service.RefereeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/referees")
@RequiredArgsConstructor
@CrossOrigin
public class RefereeController {
    private final RefereeService refereeService;

    @GetMapping
    public List<RefereeResponse> getAll() { return refereeService.getAll(); }

    @GetMapping("/{id}")
    public RefereeResponse getById(@PathVariable Long id) { return refereeService.getById(id); }

    @PostMapping
    public RefereeResponse create(@RequestBody RefereeRequest request) { return refereeService.create(request); }

    @PutMapping("/{id}")
    public RefereeResponse update(@PathVariable Long id, @RequestBody RefereeRequest request) {
        return refereeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { refereeService.delete(id); }
}
