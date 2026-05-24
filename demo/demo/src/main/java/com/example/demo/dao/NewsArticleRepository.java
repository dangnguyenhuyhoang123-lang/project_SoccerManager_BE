package com.example.demo.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsArticleRepository extends JpaRepository<com.example.demo.entity.news.NewsArticle, Long> {

    boolean existsBySourceUrl(String sourceUrl);

    Optional<com.example.demo.entity.news.NewsArticle> findBySourceUrl(String sourceUrl);

    Page<com.example.demo.entity.news.NewsArticle> findByCategoryOrderByPublishedAtDescCreatedAtDesc(
            String category,
            Pageable pageable
    );

    List<com.example.demo.entity.news.NewsArticle> findTop20ByCategoryOrderByPublishedAtDescCreatedAtDesc(
            String category
    );
    @Query("""
        SELECT n FROM NewsArticle n
        WHERE n.category = :category
        ORDER BY COALESCE(n.publishedAt, n.createdAt) DESC
    """)
    Page<com.example.demo.entity.news.NewsArticle> findLatestByCategory(
            @Param("category") String category,
            Pageable pageable
    );


    @Query("""
    SELECT n FROM NewsArticle n
    WHERE n.category = :category
      AND n.publishedAt >= :startDate
      AND n.publishedAt < :endDate
    ORDER BY n.publishedAt DESC
""")
    Page<com.example.demo.entity.news.NewsArticle> findLatestByCategoryAndPublishedYear(
            @Param("category") String category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}