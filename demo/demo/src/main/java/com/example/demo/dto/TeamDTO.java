package com.example.demo.dto;

import com.example.demo.entity.League;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeamDTO {
    private Long id;
    private String name;
    private String logo;
    private String stadium;


}