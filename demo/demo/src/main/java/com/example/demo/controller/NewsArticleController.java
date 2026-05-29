package com.example.demo.controller;

import com.example.demo.dao.NewsArticleRepository;
import com.example.demo.dto.news.NewsArticleDetailResponse;
import com.example.demo.dto.news.NewsArticleResponse;
import com.example.demo.service.crawl.VffNewsCrawlerService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsArticleController {

    private final NewsArticleRepository newsArticleRepository;
    private final VffNewsCrawlerService vffNewsCrawlerService;

    public NewsArticleController(
            NewsArticleRepository newsArticleRepository,
            VffNewsCrawlerService vffNewsCrawlerService
    ) {
        this.newsArticleRepository = newsArticleRepository;
        this.vffNewsCrawlerService = vffNewsCrawlerService;
    }


    @GetMapping("/{id}")
    public NewsArticleDetailResponse getNewsDetail(@PathVariable Long id) {
        com.example.demo.entity.news.NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết."));

        return new NewsArticleDetailResponse(article);
    }
    @GetMapping("/vff/latest")
    public List<NewsArticleResponse> getLatestVffNews(
            @RequestParam(defaultValue = "20") int limit
    ) {
        Pageable pageable = PageRequest.of(0, limit);

        LocalDateTime startDate = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2027, 1, 1, 0, 0);

        return newsArticleRepository
                .findLatestByCategoryAndPublishedYear(
                        "Hoạt động VFF",
                        startDate,
                        endDate,
                        pageable
                )
                .getContent()
                .stream()
                .map(NewsArticleResponse::new)
                .toList();
    }

    @PostMapping("/vff/crawl")
    public String crawlVffNews() {
        vffNewsCrawlerService.crawlLatestNews();
        return "Crawl VFF news successfully";
    }
}
