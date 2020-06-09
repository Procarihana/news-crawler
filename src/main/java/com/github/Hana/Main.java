package com.github.Hana;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


import javax.print.Doc;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;


public class Main {
    public static void main(String[] args) throws IOException {


        //待处理的连接池
        List<String> linkPool = new ArrayList<>();

        //已经处理的连接池
        Set<String> processLinks = new HashSet<>();

        linkPool.add("https://sina.cn");

        while (!linkPool.isEmpty()) {
            String link = linkPool.remove(linkPool.size() - 1);
            if (processLinks.contains(link)) {
                continue;
            }
            isIllegalLink(link);
            if (isInterestingLink(link)) {
                //what we need.
                Document document = httpGetAndParseHtml(link);
                //把链接里面的A标签映射成A标签里面的链接，并添加到连接池里面
                // ArrayList<Element> links = document.select("a");
                // for (Element aTag : links) {
                // linkPool.add(aTag.attr("href"));
                // }
                document.select("a").stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);
                //如果是一个详细的新闻页面就存入数据库，否则什么也不做
                storeIntoDatabaseIfItIsNewsPage(processLinks, link, document);
            } else {
                continue;
            }
        }
    }

    private static void isIllegalLink(String link) {
        if (link.contains("|")) {
            link = link.replace("|", "%7C");
        }
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
    }

    private static void storeIntoDatabaseIfItIsNewsPage(Set<String> processLinks, String link, Document document) {
        ArrayList<Element> articleTags = document.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                System.out.println(articleTags.get(0).child(0).text());

            }
            processLinks.add(link);
        }
    }

    private static Document httpGetAndParseHtml(String link) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(link);
        String html = null;
        httpGet.addHeader("USER_AGENT",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/83.0.4103.61 Safari/537.36");
        System.out.println(link);

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            html = EntityUtils.toString(entity1);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return Jsoup.parse(html);
    }

    private static boolean isInterestingLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotNeedLink(link);

    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isNotNeedLink(String link) {
        return !link.contains("passport") && !link.contains("hotnews");
    }
}
