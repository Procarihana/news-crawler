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


import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {
    public static void main(String[] args) throws IOException, SQLException {
        //数据库链接
        Connection dbConnection = DriverManager
                .getConnection("jdbc:h2:file:/Users/jinhua/IdeaProjects/news-crewler/news", "hana", "hana");
        try {
            //把待处理的链接池添加到数据库里面
            List<String> linkPool = loadUrlFromDatabase(dbConnection, "select * from links_to_be_processed");
            //把已经处理的链接放入数据库里面连接池
            //Set<String> processLinks = new HashSet<>(loadUrlFromDatabase(dbConnection, "select link from links_already_processed"));
            while (true) {
                if (linkPool.isEmpty()) {
                    System.out.println(linkPool.size());
                    break;
                }
                //处理完后从池子（包括数据库）中删除
                String link = linkPool.remove(linkPool.size() - 1);
                System.out.println(link);
                isIllegalLink(link);
                isIncompleteLink(link);
                if (isInterestingLink(link)) {
                    //询问数据库是否当前链接是不是被处理了
//                if (processLinks.contains(link)) {
//                    continue;
//                }
                    if (isLinkProcessed(dbConnection, link)) {
                        //what we need.
                        Document document = httpGetAndParseHtml(link);
                        //把链接里面的A标签映射成A标签里面的链接，并添加到连接池里面
                        parseUrlsFromPageAndStoreIntoDatabase(dbConnection, document);
                        //如果是一个详细的新闻页面就存入数据库，否则什么也不做
                        storeIntoDatabaseIfItIsNewsPage(dbConnection, link, document);
                        //处理完后就把已处理的链接从待处理的数据库中删除
                        disposeUsedLinkFromDatabase(dbConnection, link);
                        //把已处理的链接加入到已处理的数据库里面
                        insertLinkIntoDatabase(dbConnection, link, "INSERT INTO LINKS_ALREADY_PROCESSED(link) values (?)");
                    }
                }
            }
        } finally {
            System.out.println("Exit");
        }
    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection dbConnection, Document document) throws SQLException {
        for (Element aTag : document.select("a")) {
            String href = aTag.attr("href");
            insertLinkIntoDatabase(dbConnection, href, "INSERT INTO LINKS_TO_BE_PROCESSED(link) values (?)");
        }
    }

    private static boolean isLinkProcessed(Connection dbConnection, String link) {
        try (PreparedStatement statement = dbConnection.prepareStatement("SELECT link FROM LINKS_ALREADY_PROCESSED where link = ? ")) {
            statement.setString(1, link);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }

    private static void insertLinkIntoDatabase(Connection dbConnection, String attributeKey, String sql) throws SQLException {
        try (PreparedStatement statement = dbConnection.prepareStatement(sql)) {
            statement.setString(1, attributeKey);
            statement.executeUpdate();
        }
    }

    private static void disposeUsedLinkFromDatabase(Connection dbconnection, String link) {
        try (PreparedStatement statement = dbconnection
                .prepareStatement("DELETE FROM  LINKS_TO_BE_PROCESSED where link = ? ")) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<String> loadUrlFromDatabase(Connection dbConnection, String sql) throws SQLException {
        List<String> results = new ArrayList<>();
        try (PreparedStatement statement = dbConnection.
                prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return results;
    }

    private static void isIllegalLink(String link) {
        if (link.contains("|")) {
            link = link.replace("|", "%7C");
        }
    }

    private static void isIncompleteLink(String link) {
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
    }

    private static void storeIntoDatabaseIfItIsNewsPage(Connection dbConnection, String link, Document document) {
        ArrayList<Element> articleTags = document.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                System.out.println(articleTags.get(0).child(0).text());

            }
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
            System.out.println(e.toString());
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
        return link.contains("sina.cn") && isContainArticleTime(link);
    }

    private static boolean isContainArticleTime(String link) {
        String regex = "\\d{4}-\\d{1,2}-\\d{1,2}";
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(link);
        if (matcher.find()) {
            return link.contains(matcher.group(0));
        } else {
            return !link.contains(link);// !link.equal(link)
        }
    }

    private static boolean isNotNeedLink(String link) {
        return !link.contains("passport") && !link.contains("callback") && !link.contains("video") && !link.contains("auto") && !link.contains("share");
    }
}
