package com.example.demo.dto.registrationclub;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class RegistrationPlayerViewDTO {
    private String name;
    private String idCode;
    private LocalDate dateOfBirth;
    private String position;
    private Integer shirtNumber;
    private String nationality;
    private Integer height;
    private Integer weight;
    private boolean official;
}
