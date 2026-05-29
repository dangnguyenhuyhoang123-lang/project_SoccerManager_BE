package com.example.demo.controller;


import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.user.*;
import com.example.demo.entity.user.User;
import jakarta.servlet.http.HttpServletRequest;
import com.example.demo.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

        Long teamId = user.getTeam() != null ? user.getTeam().getId() : null;
        return new UserDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUserName(),
                user.getDisplayName(),
                user.getAvatar(),
                user.getStatus(),
                user.getRoles()
                        .stream()
                        .map(role -> role.getRoleName())
                        .toList(),
                teamId
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


    @PostMapping("/createUser")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PutMapping("/{userId}/info")
    public ResponseEntity<UserDTO> updateUserInfo(
            @PathVariable Long userId,
            @RequestBody UpdateUserInfoRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserInfo(userId, request));
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<UserDTO> updateUserRoles(
            @PathVariable Long userId,
            @RequestBody UpdateUserRolesRequest request
    ) {
        return ResponseEntity.ok(
                userService.updateUserRoles(
                        userId,
                        request.getRoles(),
                        request.getTeamId()
                )
        );
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<UserDTO> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(
                userService.updateUserStatus(userId, request.getStatus())
        );
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
