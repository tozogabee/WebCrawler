package org.example;

import org.example.service.WebCrawler;

public class Main {
    public static void main(String[] args) {
        WebCrawler crawler = new WebCrawler(args[0]);
        crawler.shutdown(); // Shutdown after crawling completes
        crawler.printSortedLinks();
    }
}