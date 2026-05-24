package com.example.demo.dao.registerteam;

import com.example.demo.entity.registerclub.RegistrationCoach;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationCoachRepository extends JpaRepository<RegistrationCoach,Long> {
    @EntityGraph(attributePaths = {"coach"})
    List<RegistrationCoach> findByRegistrationTeamId(Long registrationTeamId);

    long countByRegistrationTeamId(Long registrationTeamId);
}
