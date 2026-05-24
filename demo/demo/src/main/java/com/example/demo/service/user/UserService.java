package com.example.demo.service.user;

import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.UserDTO;
import com.example.demo.entity.message.ErrorMessage;
import com.example.demo.entity.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public User findByUserName(String username)
    {
        return userRepository.findByUserName(username);
    }

    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<UserDTO> getUserDtos(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toUserDto);
    }

    public ResponseEntity<?> register(User user) {
        if (userRepository.existsByUserName(user.getUserName())) {
            return ResponseEntity.badRequest().body(new ErrorMessage("username", "TÃªn Ä‘Äƒng nháº­p Ä‘Ã£ tá»“n táº¡i"));
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(new ErrorMessage("email", "Email Ä‘Ã£ tá»“n táº¡i"));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("Success");
    }

    public ResponseEntity<?> login(String username, String password, HttpServletRequest request) {
        User user = userRepository.findByUserName(username);

        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage("username", "Username không tồn tại"));
        }

        try {
            upgradeLegacyPasswordIfNeeded(user, password);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage("password", "Password không chính xác"));
        }

        return ResponseEntity.ok(buildLoginResponse(user));
    }

    private void upgradeLegacyPasswordIfNeeded(User user, String rawPassword) {
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return;
        }

        boolean alreadyEncoded;
        try {
            alreadyEncoded = passwordEncoder.matches(rawPassword, storedPassword);
        } catch (IllegalArgumentException ex) {
            alreadyEncoded = false;
        }

        if (alreadyEncoded) {
            return;
        }

        if (storedPassword.equals(rawPassword)) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
        }
    }

    private Map<String, Object> buildLoginResponse(User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("userName", user.getUserName());
        response.put("fullName", user.getFullName());
        response.put("displayName", user.getDisplayName());
        response.put("email", user.getEmail());
        response.put("status", user.getStatus());
        response.put("roles", user.getRoles().stream().map(role -> role.getRoleName()).toList());
        return response;
    }

    private UserDTO toUserDto(User user) {
        return new UserDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUserName(),
                user.getDisplayName(),
                user.getStatus(),
                user.getRoles().stream().map(role -> role.getRoleName()).toList(),
                user.getTeam().getId()
        );
    }
}
