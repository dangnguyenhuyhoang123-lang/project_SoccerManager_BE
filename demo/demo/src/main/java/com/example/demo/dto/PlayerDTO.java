package com.example.demo.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerDTO {

    private Long id;


    private String name;


    private String position;


    private Integer number;


    private String avatar;
}
