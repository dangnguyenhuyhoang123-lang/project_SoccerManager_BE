package com.example.demo.dto.referee;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RefereeRequest {
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
