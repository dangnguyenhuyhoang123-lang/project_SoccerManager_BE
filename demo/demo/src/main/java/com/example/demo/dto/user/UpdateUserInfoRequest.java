package com.example.demo.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserInfoRequest {
    private String fullName;
    private String displayName;
    private String email;
    private String avatar;
}
