package com.example.demo.dto.user;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateUserRequest {
    private String username;
    private String password;
    private String fullName;
    private String displayName;
    private String email;
    private String phone;

    private Boolean status;

    private List<String> roles;
    private Long teamId;
}