package com.example.demo.service;

import com.example.demo.dao.user.RoleRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.entity.user.Role;
import com.example.demo.entity.user.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class MyUserServiceImpl implements MyUserService {


    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;



    public MyUserServiceImpl(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public User findByUserName(String username) {
        return userRepository.findByUserName(username);
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUserName(username);

        if (user == null) {
            throw new UsernameNotFoundException("Tài khoản không tồn tại");
        }

        Set<Role> safeRoles = new HashSet<>(user.getRoles());

        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getPassword(),
                safeRoles.stream()
                        .map(r -> new SimpleGrantedAuthority(normalizeRoleName(r.getRoleName())))
                        .toList()
        );
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "ROLE_USER";
        }

        return roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
    }
}
