package com.example.demo.dao.registerteam;

import com.example.demo.entity.registerclub.RegistrationPlayer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationPlayerRepository extends JpaRepository<RegistrationPlayer,Long> {
    @EntityGraph(attributePaths = {"player"})
    List<RegistrationPlayer> findByRegistrationTeamId(Long registrationTeamId);

    long countByRegistrationTeamId(Long registrationTeamId);
}
