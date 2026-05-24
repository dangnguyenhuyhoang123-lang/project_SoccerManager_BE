package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoundDTO {
    private Integer id;
    private Integer roundNumber;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private Long seasonId;
}
