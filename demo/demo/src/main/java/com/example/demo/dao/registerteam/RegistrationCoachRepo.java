package com.example.demo.dao.registerteam;

import com.example.demo.entity.registerclub.RegistrationCoach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationCoachRepo extends JpaRepository<RegistrationCoach,Long> {
}
