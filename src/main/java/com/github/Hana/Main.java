package com.github.Hana;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.stream.Collectors;


public class Main {

    private static final String USER_NAME = "hana";
    private static final String PASS_WORD = "hana";

    public static void main(String[] args) throws SQLException, IOException {
        //数据库链接
        Connection dbConnection = DriverManager
                .getConnection("jdbc:h2:file:/Users/jinhua/IdeaProjects/news-crewler/news", USER_NAME, PASS_WORD);
        String link;
        //从数据库中加载下一个链接，如果能加载到。则进行循环
        while ((link = getNextLinkAndDelete(dbConnection)) != null) {
            //先从数据库里面拿一个链接出来（拿出来并从数据库中删除掉），准备处理
            //把待处理的链接池添加到数据库里面
            if (isLinkProcessed(dbConnection, link)) {
                continue;
            }
            if (isInterestingLink(link)) {
                //what we need.
                Document document = httpGetAndParseHtml(link);
                //把链接里面的A标签映射成A标签里面的链接，并添加到连接池里面
                parseUrlsFromPageAndStoreIntoDatabase(dbConnection, document);
                //如果是一个详细的新闻页面就存入数据库，否则什么也不做
                storeIntoDatabaseIfItIsNewsPage(dbConnection, link, document);
            } else {
                System.out.println("NN:" + link);
            }
        }
        System.out.println("Exit");
    }


    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection dbConnection, Document document) throws SQLException {

        for (Element aTag : document.select("a")) {
            String href = aTag.attr("href");
            if (href.contains("|")) {
                href = href.replace("|", "%7C");
                if (href.startsWith("//")) {
                    href = "https:" + href;
                }
            }
            if (!href.toLowerCase().startsWith("javascript")) {
                updateDatabate(dbConnection, href, "INSERT INTO LINKS_TO_BE_PROCESSED(link) values (?)");
            }
        }
    }

    private static boolean isLinkProcessed(Connection dbConnection, String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = dbConnection.prepareStatement("SELECT link FROM LINKS_ALREADY_PROCESSED where link = ? ")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
            return false;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
    }

    private static void updateDatabate(Connection dbconnection, String value, String sql) throws SQLException {
        try (PreparedStatement statement = dbconnection
                .prepareStatement(sql)) {
            statement.setString(1, value);
            statement.executeUpdate();
        }
    }


    private static String getNextLink(Connection dbConnection, String sql) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = dbConnection.
                prepareStatement(sql)) {
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return null;
    }


    private static String getNextLinkAndDelete(Connection dbConnection) throws SQLException {
        String link = getNextLink(dbConnection, "select * from links_to_be_processed LIMIT 1");
        if (link != null) {
            updateDatabate(dbConnection, link, "DELETE FROM LINKS_TO_BE_PROCESSED where link = ?");
        }
        return link;
    }
    private static void storeIntoDatabaseIfItIsNewsPage(Connection dbConnection, String link, Document document) throws SQLException {
        ArrayList<Element> articleTags = document.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                try(PreparedStatement statement = dbConnection
                        .prepareStatement("insert into news(url,title,content,created_at,modified_at) values(?,?,?,now(),now())")){
                      statement.setString(1,link);
                      statement.setString(2,title);
                      statement.setString(3,content);
                      statement.executeUpdate();
                }
                System.out.println(link);
                System.out.println(title);
                updateDatabate(dbConnection, link, "INSERT INTO LINKS_ALREADY_PROCESSED(link) values (?)");
            }
        }
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("USER_AGENT",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/83.0.4103.61 Safari/537.36");
        String html;
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            html = EntityUtils.toString(entity1);
            return Jsoup.parse(html);
        }
    }

    private static boolean isInterestingLink(String link) {
        return isNotNeedLink(link) && (isNewsPage(link) || isIndexPage(link));

    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("sina.cn") && isContainArticleTime(link);
    }

    private static boolean isContainArticleTime(String link) {
        String result;
        String regex = "\\d{4}-\\d{1,2}-\\d{1,2}";
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(link);
        if (matcher.find()) {
            result = matcher.group(0);
            return link.contains(result);
        }
        return !link.contains(link); // !link.equal(link)

    }

    private static boolean isNotNeedLink(String link) {
        return !link.contains("passport") && !link.contains("callback") && !link.contains("video") && !link.contains("auto") && !link.contains("share") && !link.contains("games");
    }
}

