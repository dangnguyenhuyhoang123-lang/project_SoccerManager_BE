package com.example.demo.dto.user;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRolesRequest {
    private List<String> roles;
    private Long teamId;
}
