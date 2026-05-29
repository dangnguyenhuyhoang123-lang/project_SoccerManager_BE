package com.example.demo.dao.user;

import com.example.demo.entity.user.Role;
import com.example.demo.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;


@RepositoryRestResource(path = "user")
public interface UserRepository extends JpaRepository<User,Long> {

    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.userName = :username")
    User findByUserName(String username);

    @Query("""
        SELECT DISTINCT u
        FROM User u
        JOIN u.roles r
        WHERE u.team.id = :teamId
          AND r.roleName = :roleName
    """)
    Optional<User> findClubManagerByTeamIdAndRoleName(
            @Param("teamId") Long teamId,
            @Param("roleName") String roleName
    );

    @Query("""
        SELECT u
        FROM User u
        WHERE u.team.id = :teamId
    """)
    Optional<User> findFirstByTeamId(@Param("teamId") Long teamId);

    Optional<User> findFirstByTeamIdAndRolesContaining(Long teamId, Role role);

    @Query("""
    SELECT DISTINCT u
    FROM User u
    JOIN u.roles r
    WHERE r.roleName = :roleName
""")
    List<User> findUsersByRoleName(@Param("roleName") String roleName);




}
