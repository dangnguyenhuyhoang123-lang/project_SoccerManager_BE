package com.example.demo.dto.registrationclub;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;


@Data
public class CoachRegistrationDTO {

    private Long coachId;
    private String role;


}
