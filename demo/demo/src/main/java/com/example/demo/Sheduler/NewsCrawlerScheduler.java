package com.example.demo.Sheduler;

import com.example.demo.service.VffNewsCrawlerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsCrawlerScheduler {

    private final VffNewsCrawlerService vffNewsCrawlerService;

    public NewsCrawlerScheduler(VffNewsCrawlerService vffNewsCrawlerService) {
        this.vffNewsCrawlerService = vffNewsCrawlerService;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void crawlVffNews() {
        vffNewsCrawlerService.crawlLatestNews();
    }
}
