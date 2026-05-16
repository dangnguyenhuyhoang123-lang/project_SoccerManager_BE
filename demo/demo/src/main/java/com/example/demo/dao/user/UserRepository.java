package com.example.demo.dao.user;

import com.example.demo.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@RepositoryRestResource(path = "user")
public interface UserRepository extends JpaRepository<User,Long> {

    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.userName = :username")
    User findByUserName(String username);


}
