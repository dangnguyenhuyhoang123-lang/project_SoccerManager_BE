package com.example.demo.dto.referee;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefereeResponse {
    private Long id;
    private String name;
    private LocalDate dateOfBirth;
    private Integer birthYear;
    private String nationality;
    private String phone;
    private String email;
    private String level;
    private String certification;
    private String avatar;
    private String status;
    private String note;
}
