package com.example.demo.service.crawl;

import com.example.demo.dao.NewsArticleRepository;
import com.example.demo.entity.news.NewsArticle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VffNewsCrawlerService {

    private static final String VFF_NEWS_URL =
            "https://www.vff.org.vn/chuyen-muc/tin-tuc/hoat-dong-vff/";

    private final NewsArticleRepository newsArticleRepository;

    public VffNewsCrawlerService(NewsArticleRepository newsArticleRepository) {
        this.newsArticleRepository = newsArticleRepository;
    }

    public void crawlLatestNews() {
        try {
            Document doc = Jsoup.connect(VFF_NEWS_URL)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            Set<String> articleUrls = extractLatestArticleUrls(doc);

            for (String articleUrl : articleUrls) {
                NewsArticle crawledArticle = crawlArticleDetail(articleUrl);

                if (crawledArticle == null) {
                    continue;
                }

                if (crawledArticle.getPublishedAt() == null
                        || crawledArticle.getPublishedAt().getYear() != 2026) {
                    continue;
                }
                NewsArticle article = newsArticleRepository
                        .findBySourceUrl(crawledArticle.getSourceUrl())
                        .orElse(new NewsArticle());

                article.setTitle(crawledArticle.getTitle());
                article.setSummary(crawledArticle.getSummary());
                article.setSourceUrl(crawledArticle.getSourceUrl());
                article.setImageUrl(crawledArticle.getImageUrl());
                article.setCategory(crawledArticle.getCategory());
                article.setPublishedAt(crawledArticle.getPublishedAt());
                article.setContent(crawledArticle.getContent());
                article.setSourceName(crawledArticle.getSourceName());

                newsArticleRepository.save(article);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Set<String> extractLatestArticleUrls(Document doc) {
        Set<String> urls = new LinkedHashSet<>();

        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String title = link.text().trim();
            String url = link.absUrl("href");

            if (title.isBlank()) continue;
            if (!url.startsWith("https://www.vff.org.vn/")) continue;
            if (url.contains("/chuyen-muc/")) continue;
            if (url.contains("/page/")) continue;
            if (url.contains("#")) continue;
            if (title.length() < 25) continue;

            // Chặn một số bài cũ ở vùng nổi bật/sidebar
            if (url.contains("danh-sach-cac-don-vi-tang-thuong")) continue;
            if (url.contains("hlv-park-hang-seo-u23-viet-nam")) continue;
            if (url.contains("kao-viet-nam")) continue;
            if (url.contains("afc-xac-dinh-quoc-gia-chu-nha")) continue;
            if (url.contains("hoc-vien-juventus-viet-nam")) continue;
            if (url.contains("cau-thu-viet-kieu")) continue;

            urls.add(url);

            if (urls.size() >= 20) {
                break;
            }
        }

        return urls;
    }

    private NewsArticle crawlArticleDetail(String articleUrl) {
        try {
            Document doc = Jsoup.connect(articleUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            Element titleElement = doc.selectFirst("h1");

            if (titleElement == null) {
                return null;
            }

            String title = titleElement.text().trim();

            if (title.isBlank()) {
                return null;
            }

            String imageUrl = doc.select("meta[property=og:image]").attr("content");

            String content = extractContent(doc);
            String summary = extractSummary(doc, content);

            NewsArticle article = new NewsArticle();
            article.setTitle(title);
            article.setSummary(summary);
            article.setSourceUrl(articleUrl);
            article.setImageUrl(imageUrl);
            article.setCategory("Hoạt động VFF");
            article.setSourceName("VFF");
            article.setPublishedAt(extractPublishedAt(doc));
            article.setContent(content);

            return article;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractContent(Document doc) {
        Element contentElement = doc.selectFirst(
                ".entry-content, .post-content, .td-post-content, article"
        );

        if (contentElement == null) {
            return "";
        }

        return contentElement.select("p").eachText()
                .stream()
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .filter(text -> text.length() > 20)
                .filter(text -> !text.contains("©"))
                .filter(text -> !text.contains("Ghi rõ nguồn"))
                .filter(text -> !text.contains("Trưởng ban biên tập"))
                .filter(text -> !text.contains("Phó ban biên tập"))
                .filter(text -> !text.contains("Địa chỉ:"))
                .filter(text -> !text.contains("Điện thoại:"))
                .filter(text -> !text.contains("Bản quyền thuộc về"))
                .filter(text -> !text.contains("Liên đoàn Bóng đá Việt Nam - VFF"))
                .collect(Collectors.joining("\n"));
    }

    private String extractSummary(Document doc, String content) {
        String summary = doc.select("meta[property=og:description]").attr("content");

        if (summary == null
                || summary.isBlank()
                || summary.equalsIgnoreCase("Liên đoàn Bóng đá Việt Nam")) {

            String[] paragraphs = content.split("\n");

            if (paragraphs.length > 0) {
                summary = paragraphs[0];
            }
        }

        if (summary == null) {
            return "";
        }

        if (summary.length() > 300) {
            summary = summary.substring(0, 300) + "...";
        }

        return summary;
    }

    private LocalDateTime extractPublishedAt(Document doc) {
        String dateText = doc.select("meta[property=article:published_time]").attr("content");

        if (dateText != null && !dateText.isBlank()) {
            try {
                return OffsetDateTime.parse(dateText).toLocalDateTime();
            } catch (Exception ignored) {
            }
        }

        Element timeElement = doc.selectFirst("time");

        if (timeElement != null) {
            String timeText = timeElement.hasAttr("datetime")
                    ? timeElement.attr("datetime")
                    : timeElement.text();

            try {
                return OffsetDateTime.parse(timeText).toLocalDateTime();
            } catch (Exception ignored) {
            }

            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return LocalDateTime.parse(timeText.trim(), formatter);
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}