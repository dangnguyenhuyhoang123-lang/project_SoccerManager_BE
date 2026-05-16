package com.example.demo.service;

import com.example.demo.entity.user.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface MyUserService extends UserDetailsService {
    public User findByUserName(String username);
}
