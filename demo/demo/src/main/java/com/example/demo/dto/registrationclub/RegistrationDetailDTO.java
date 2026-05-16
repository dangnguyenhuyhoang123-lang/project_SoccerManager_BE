package com.example.demo.dto.registrationclub;

import com.example.demo.entity.GrassType;
import com.example.demo.entity.registerclub.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class RegistrationDetailDTO {
    private Long id;
    private Long seasonId;
    private String seasonName;
    private String teamName;
    private String logo;
    private Integer establishedYear;
    private String city;
    private String region;
    private String owner;
    private String description;
    private String stadiumName;
    private String stadiumAddress;
    private Integer stadiumCapacity;
    private GrassType stadiumGrass;
    private RegistrationStatus status;
    private String note;
    private LocalDateTime submittedAt;
    private List<RegistrationPlayerViewDTO> players;
    private List<RegistrationCoachViewDTO> coaches;
}
