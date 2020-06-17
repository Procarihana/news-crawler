package com.github.Hana;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLinkAndDelete() throws SQLException;

    void insertNewsIntoDatabaseAndInsertLinkIntoAlreadyProcessed(String url, String title, String content) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void insertLinkIntoToBeProcessed(String href) throws SQLException;
}
