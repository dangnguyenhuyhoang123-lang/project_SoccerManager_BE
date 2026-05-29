package com.example.demo.dao.player;

import com.example.demo.entity.Player;
import com.example.demo.entity.PlayerSeason;
import com.example.demo.entity.SeasonTeam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Page<Player> findByPosition(String position, Pageable pageable);

    Page<Player> findByStatus(String status, Pageable pageable);

    Page<Player> findByPositionAndStatus(String position, String status, Pageable pageable);

    Optional<Player> findByIDCode(String idCode);

    Page<Player> findByTeamId(Long teamId, Pageable pageable );

    Optional<Player> findBySourceUrl(String sourceUrl);

    Optional<Player> findByNormalizedName(String normalizedName);


    Optional<Player> findByVpfPlayerSlug(String vpfPlayerSlug);

    Optional<Player> findByNormalizedNameAndDateOfBirth(String normalizedName, LocalDate dateOfBirth);


}
