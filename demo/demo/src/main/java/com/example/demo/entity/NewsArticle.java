package com.example.demo.entity.news;

import com.example.demo.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "news_articles")
public class NewsArticle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 1000)
    private String summary;

    @Column(nullable = false, unique = true, length = 1000)
    private String sourceUrl;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 255)
    private String category;

    private LocalDateTime publishedAt;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(length = 100)
    private String sourceName = "VFF";
}