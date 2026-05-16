package com.example.demo.dto.registrationclub;

import com.example.demo.entity.registerclub.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RegistrationSummaryDTO {
    private Long id;
    private Long seasonId;
    private String seasonName;
    private String teamName;
    private String city;
    private RegistrationStatus status;
    private Integer playerCount;
    private Integer coachCount;
    private LocalDateTime submittedAt;
    private String note;
}
