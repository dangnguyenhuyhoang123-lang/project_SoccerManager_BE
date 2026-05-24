package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerUpsertDTO {
    private String name;
    private String idCode;
    private String avatar;
    private LocalDate dateOfBirth;
    private String position;
    private String detailPosition;
    private Integer shirtNumber;
    private String nationality;
    private Integer height;
    private Integer weight;
    private String status;
    private Long teamId;
}
