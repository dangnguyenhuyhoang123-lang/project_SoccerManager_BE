package com.example.demo.dao.user;

import com.example.demo.entity.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "role")
public interface RoleRepository extends JpaRepository<Role,Long> {
}
