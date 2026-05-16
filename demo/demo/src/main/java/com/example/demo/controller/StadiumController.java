package com.example.demo.controller;

import com.example.demo.entity.GrassType;
import com.example.demo.service.StadiumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stadiums")
@CrossOrigin
public class StadiumController {

    private StadiumService stadiumService;

    @Autowired
    public StadiumController(StadiumService stadiumService) {
        this.stadiumService = stadiumService;
    }

    @GetMapping
    public List<StadiumResponse> getStadiums(@RequestParam(required = false) String search) {
        return stadiumService.getStadiums(search);
    }

    @GetMapping("/{id}")
    public StadiumResponse getStadium(@PathVariable Long id) {
        return stadiumService.getStadium(id);
    }

    @PostMapping
    public StadiumResponse createStadium(@RequestBody StadiumRequest request) {
        return stadiumService.create(request);
    }

    @PutMapping("/{id}")
    public StadiumResponse updateStadium(@PathVariable Long id, @RequestBody StadiumRequest request) {
        return stadiumService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteStadium(@PathVariable Long id) {
        stadiumService.delete(id);
    }

    public record StadiumRequest(String name, String address, Integer capacity, GrassType grass) {
    }

    public record StadiumResponse(Long id, String name, String address, Integer capacity, GrassType grass) {
    }
}
