package com.example.demo.dao.team;

import com.example.demo.entity.team.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StadiumRepository extends JpaRepository<Stadium, Long> {
    Optional<Stadium> findByNameIgnoreCaseAndAddressIgnoreCase(String name, String address);

    List<Stadium> findByNameContainingIgnoreCase(String name);

    Optional<Stadium> findByNameIgnoreCase(String name);

    Optional<Stadium> findByNormalizedName(String normalizedName);
}
