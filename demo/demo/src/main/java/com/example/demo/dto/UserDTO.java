package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String fullName;
    private String email;
    private String username;
    private String displayName;
    private Boolean status;
    private List<String> roles;
    private Long teamId;
}
