package com.example.demo.service.user;

import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dao.user.RoleRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.user.CreateUserRequest;
import com.example.demo.dto.user.UpdateUserInfoRequest;
import com.example.demo.dto.user.UserDTO;
import com.example.demo.entity.Team;
import com.example.demo.entity.message.ErrorMessage;
import com.example.demo.entity.user.Role;
import com.example.demo.entity.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;
    private final TeamRepository teamRepository;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       RoleRepository roleRepository,
                       TeamRepository teamRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.roleRepository = roleRepository;
        this.teamRepository=teamRepository;
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
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage("username", "Tên đăng nhập đã tồn tại"));
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage("email", "Email đã tồn tại"));
        }

        Role defaultRole = roleRepository.findByRoleName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER chưa tồn tại trong database"));

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(true);

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        } else {
            user.getRoles().clear();
        }

        user.getRoles().add(defaultRole);

        userRepository.save(user);
        return ResponseEntity.ok("Success");
    }

    public ResponseEntity<?> login(String username, String password, HttpServletRequest request) {
        User user = userRepository.findByUserName(username);

        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage("username", "Username không tồn tại"));
        }
        if (Boolean.FALSE.equals(user.getStatus())) {
            return ResponseEntity.status(403)
                    .body(new ErrorMessage("status", "Tài khoản đã bị vô hiệu hóa"));
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
        response.put("teamId", user.getTeam() != null ? user.getTeam().getId() : null);
        return response;
    }
    @Transactional
    public ResponseEntity<?> createUser(CreateUserRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage("username", "Tên đăng nhập không được để trống"));
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage("password", "Mật khẩu không được để trống"));
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage("email", "Email không được để trống"));
        }

        if (userRepository.existsByUserName(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage("username", "Tên đăng nhập đã tồn tại"));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage("email", "Email đã tồn tại"));
        }

        User user = new User();

        user.setUserName(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setDisplayName(request.getDisplayName());
        user.setEmail(request.getEmail());
        user.setStatus(request.getStatus() != null ? request.getStatus() : true);

        List<String> requestedRoles =
                request.getRoles() == null || request.getRoles().isEmpty()
                        ? List.of("ROLE_USER")
                        : request.getRoles();

        Set<Role> roles = requestedRoles.stream()
                .map(roleName -> roleRepository.findByRoleName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleName)))
                .collect(Collectors.toSet());

        user.setRoles(roles);

        boolean isClubManager = requestedRoles.contains("ROLE_CLUB_MANAGER");

        if (isClubManager) {
            if (request.getTeamId() == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorMessage("teamId", "Club Manager phải được gán với một đội"));
            }

            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đội id = " + request.getTeamId()));

            user.setTeam(team);
        } else {
            user.setTeam(null);
        }

        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(toUserDto(savedUser));
    }


    public UserDTO updateUserRoles(Long userId, List<String> roleNames, Long teamId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Set<Role> roles = roleNames.stream()
                .map(roleName -> roleRepository.findByRoleName(roleName)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy role: " + roleName)))
                .collect(Collectors.toSet());

        boolean isClubManager = roleNames.contains("ROLE_CLUB_MANAGER");

        if (isClubManager) {
            if (teamId == null) {
                throw new RuntimeException("Khi cấp quyền CLUB_MANAGER, bắt buộc phải chọn đội bóng");
            }

            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng"));

            user.setTeam(team);
        } else {
            user.setTeam(null);
        }

        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        return toUserDto(savedUser);
    }

    public UserDTO updateUserStatus(Long userId, Boolean status) {
        if (status == null) {
            throw new RuntimeException("Trạng thái tài khoản không được để trống");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setStatus(status);

        User savedUser = userRepository.save(user);
        return toUserDto(savedUser);
    }
    @Transactional
    public UserDTO updateUserInfo(Long userId, UpdateUserInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng id = " + userId));

        user.setFullName(request.getFullName());
        user.setDisplayName(request.getDisplayName());
        user.setEmail(request.getEmail());
        user.setAvatar(request.getAvatar());

        User savedUser = userRepository.save(user);

        Long teamId = savedUser.getTeam() != null ? savedUser.getTeam().getId() : null;

        return new UserDTO(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getUserName(),
                savedUser.getDisplayName(),
                savedUser.getAvatar(),
                savedUser.getStatus(),
                savedUser.getRoles()
                        .stream()
                        .map(role -> role.getRoleName())
                        .toList(),
                teamId
        );
    }
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        userRepository.delete(user);
    }


    private UserDTO toUserDto(User user) {
        Long teamId = user.getTeam() != null ? user.getTeam().getId() : null;

        return new UserDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUserName(),
                user.getDisplayName(),
                user.getAvatar(),
                user.getStatus(),
                user.getRoles().stream().map(Role::getRoleName).toList(),
                teamId
        );
    }
}
