package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeagueDTO {
    private Long id;
    private String name;
    private String country;
    private String scale;
    private String status;
}
