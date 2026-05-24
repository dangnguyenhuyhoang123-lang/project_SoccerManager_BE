package com.example.demo.dto;

import com.example.demo.entity.GrassType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StadiumDTO {
    private Long id;
    private String name;
    private String address;
    private Integer capacity;
    private GrassType grass;
}
