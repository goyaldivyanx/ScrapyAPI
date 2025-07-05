package org.divyansh.controller;

import org.divyansh.OneMgScraper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScraperController {

    @GetMapping("/1mg-deal")
    public Map<String, String> get1mgDeal(
            @RequestParam String medicine,
            @RequestParam String city) {
        return OneMgScraper.get1mgBestDeal(medicine, city);
    }
    @GetMapping("/Test")
    public void test() {
       System.out.println("Test Completelted");
    }
}
