package com.github.Hana;

public class Main {
    public static void main(String[] args) {
        CrawlerDao crawlerDao = new MybatisCrawlerDao();
        for (int i = 0; i <4 ; i++) {
            new Crawler(crawlerDao).start();

        }

    }
}
