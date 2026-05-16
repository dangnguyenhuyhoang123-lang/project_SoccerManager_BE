package com.example.demo.dto.registrationclub;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PlayerRegistrationDTO {

    private Long playerId;   // ID của Player trong DB
    private Integer shirtNumber;
    private String position;
}
