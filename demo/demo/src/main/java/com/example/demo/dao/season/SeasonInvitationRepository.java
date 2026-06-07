package com.example.demo.dao.season;

import com.example.demo.entity.registerclub.InvitationStatus;
import com.example.demo.entity.registerclub.SeasonInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeasonInvitationRepository extends JpaRepository<SeasonInvitation, Long> {

    List<SeasonInvitation> findBySeasonId(Long seasonId);

    Optional<SeasonInvitation> findBySeasonIdAndTeamId(Long seasonId, Long teamId);

    boolean existsBySeasonIdAndTeamIdAndStatusIn(
            Long seasonId,
            Long teamId,
            Collection<InvitationStatus> statuses
    );

    boolean existsBySeasonIdAndTeamId(
            Long seasonId,
            Long teamId
    );


    List<SeasonInvitation> findByTeamIdOrderByInvitedAtDesc(Long teamId);

    List<SeasonInvitation> findByTeamIdAndStatusOrderByInvitedAtDesc(
            Long teamId,
            InvitationStatus status
    );
}
