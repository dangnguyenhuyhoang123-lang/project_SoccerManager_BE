package com.example.demo.dto.registrationclub;

import com.example.demo.entity.GrassType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class StadiumRegistrationDTO {

    private String name;


    private String address;


    private Integer capacity;


    private GrassType grass;
}
