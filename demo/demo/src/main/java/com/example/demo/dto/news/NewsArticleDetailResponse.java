package com.example.demo.dto.news;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NewsArticleDetailResponse {

    private Long id;
    private String title;
    private String summary;
    private String sourceUrl;
    private String imageUrl;
    private String category;
    private LocalDateTime publishedAt;
    private String content;
    private String sourceName;
    private LocalDateTime createdAt;

    public NewsArticleDetailResponse(com.example.demo.entity.news.NewsArticle article) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.summary = article.getSummary();
        this.sourceUrl = article.getSourceUrl();
        this.imageUrl = article.getImageUrl();
        this.category = article.getCategory();
        this.publishedAt = article.getPublishedAt();
        this.content = article.getContent();
        this.sourceName = article.getSourceName();
        this.createdAt = article.getCreatedAt();
    }
}
