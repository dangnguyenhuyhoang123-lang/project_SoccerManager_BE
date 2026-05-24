package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeasonDTO {
    private Long id;
    private String year;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private LeagueDTO league;
}
