package com.example.demo.dao;

import com.example.demo.entity.Standing;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StandingRepository extends JpaRepository<Standing,Long> {
    // Lấy bảng xếp hạng của một mùa giải, sắp xếp theo thứ tự ưu tiên
    @EntityGraph(attributePaths = {"team"})
    List<Standing> findBySeasonIdOrderByPointsDescGoalDifferenceDescGoalsForDesc(Long seasonId);

    // Tìm bản ghi của một đội trong mùa giải để cập nhật sau trận đấu
    Optional<Standing> findBySeasonIdAndTeamId(Long seasonId, Long teamId);

    // Dùng để kiểm tra sự tồn tại trước khi khởi tạo
    boolean existsBySeasonIdAndTeamId(Long seasonId, Long teamId);

}
