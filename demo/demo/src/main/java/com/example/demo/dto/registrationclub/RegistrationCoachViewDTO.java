package com.example.demo.dto.registrationclub;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class RegistrationCoachViewDTO {
    private String name;
    private String nationality;
    private String idCode;
    private LocalDate birthDay;
    private String role;
    private String description;
}
