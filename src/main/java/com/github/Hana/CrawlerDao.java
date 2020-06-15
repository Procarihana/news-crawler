package com.github.Hana;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLink(String sql) throws SQLException;

    String getNextLinkAndDelete() throws SQLException;

    void updateDatabate(String value, String sql) throws SQLException;

    void insertNewsIntoDatabase(String url, String title, String content) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;
}
