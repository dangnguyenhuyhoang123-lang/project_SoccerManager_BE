package com.example.demo.dao.registerteam;

import com.example.demo.entity.registerclub.RegistrationStatus;
import com.example.demo.entity.registerclub.RegistrationTeam;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationTeamRepository extends JpaRepository<RegistrationTeam, Long> {

    @EntityGraph(attributePaths = {"season", "team"})
    List<RegistrationTeam> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"season", "team"})
    List<RegistrationTeam> findByStatusOrderByCreatedAtDesc(RegistrationStatus status);

    @EntityGraph(attributePaths = {"season", "team", "team.stadium"})
    Optional<RegistrationTeam> findOneById(Long id);

    boolean existsBySeasonIdAndNameAtRegistrationIgnoreCaseAndStatusIn(
            Long seasonId,
            String nameAtRegistration,
            List<RegistrationStatus> statuses
    );

    long countBySeasonIdAndStatus(Long seasonId, RegistrationStatus status);

    // 2. Dùng ở dòng 141 (Kiểm tra xem Team này đã nộp đơn nào đang chờ duyệt hoặc đã duyệt chưa)
    boolean existsBySeasonIdAndTeamIdAndStatusIn(Long seasonId, Long teamId, List<RegistrationStatus> statuses);
}
