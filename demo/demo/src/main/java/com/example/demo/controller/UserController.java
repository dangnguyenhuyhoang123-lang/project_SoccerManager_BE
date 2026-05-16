package com.example.demo.controller;


import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.UserDTO;
import com.example.demo.entity.user.User;
import jakarta.servlet.http.HttpServletRequest;
import com.example.demo.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-account")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {

    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Page<UserDTO> getUsers(Pageable pageable) {
        return userService.getUserDtos(pageable);
    }

    @GetMapping("/me")
    public UserDTO getCurrentUser(Authentication authentication) {

//        if (authentication == null || !authentication.isAuthenticated()) {
//            throw new RuntimeException("Unauthenticated");
//        }

        String username = authentication.getName();

        User user = userService.findByUserName(username);

        return new UserDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUserName(),
                user.getDisplayName(),
                user.getStatus(),
                user.getRoles()
                        .stream()
                        .map(role -> role.getRoleName())
                        .toList()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return userService.login(request.getUsername(), request.getPassword(), httpRequest);
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated  @RequestBody User user) {
        return userService.register(user);
    }


}
