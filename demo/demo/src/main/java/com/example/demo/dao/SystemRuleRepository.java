package com.example.demo.dao;

import com.example.demo.entity.SystemRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SystemRuleRepository extends JpaRepository<SystemRule,Long> {
    boolean existsByRuleNameIgnoreCase(String ruleName);

    boolean existsByRuleNameIgnoreCaseAndIdNot(String ruleName, Long id);
}
