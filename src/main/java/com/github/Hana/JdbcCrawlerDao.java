package com.github.Hana;

import java.sql.*;


public class JdbcCrawlerDao implements CrawlerDao {
    private final String USER_NAME = "hana";
    private final String PASS_WORD = "hana";
    //数据库链接
    private final Connection dbConnection;

    public JdbcCrawlerDao() {
        try {
            this.dbConnection = DriverManager
                    .getConnection("jdbc:h2:file:/Users/jinhua/IdeaProjects/news-crewler/news", USER_NAME, PASS_WORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getNextLinkAndDelete() throws SQLException {
        String link = getNextLink("select * from links_to_be_processed LIMIT 1");
        if (link != null) {
            updateDatabase(link, "DELETE FROM LINKS_TO_BE_PROCESSED where link = ?");
        }
        return link;
    }

    public String getNextLink(String sql) throws SQLException {
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

    public void updateDatabase(String value, String sql) throws SQLException {
        try (PreparedStatement statement = dbConnection
                .prepareStatement(sql)) {
            statement.setString(1, value);
            statement.executeUpdate();
        }
    }

    public void insertNewsIntoDatabaseAndInsertLinkIntoAlreadyProcessed(String url, String title, String content) throws SQLException {
        try (PreparedStatement statement = dbConnection
                .prepareStatement("insert into news(url,title,content,created_at,modified_at) values(?,?,?,now(),now()")) {
            statement.setString(1, url);
            statement.setString(2, title);
            statement.setString(3, content);
            statement.executeUpdate();
        }
        updateDatabase(url, "INSERT INTO LINKS_ALREADY_PROCESSED(link) values (?)");
    }

    public boolean isLinkProcessed(String link) throws SQLException {
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

    public void insertLinkIntoToBeProcessed(String href) throws SQLException {
       updateDatabase(href,"INSERT INTO LINKS_TO_BE_PROCESSED(link) values (?)");
    }
}
