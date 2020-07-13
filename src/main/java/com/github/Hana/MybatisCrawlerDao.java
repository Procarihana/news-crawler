package com.github.Hana;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MybatisCrawlerDao implements CrawlerDao {
    private SqlSessionFactory sqlSessionFactory;


    public MybatisCrawlerDao() {
        try {
            String resource = "db/Mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory =
                    new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized String getNextLinkAndDelete() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String link = (String) session.selectOne(
                    "com.github.Hana.MyMapper.selectNextAvailableLink");
            if (link != null) {
                session.delete("com.github.Hana.MyMapper.deleteNextAvailableLink",link);
            }
            return link;
        }
    }

    @Override
    public void insertNewsIntoDatabaseAndInsertLinkIntoAlreadyProcessed(String link, String title, String content) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.Hana.MyMapper.insertNews", new News(title, content,link));
        }
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "links_already_processed");
        param.put("link",link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.Hana.MyMapper.insertLink", param);
        }


    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            Integer count = (Integer) session.selectOne("com.github.Hana.MyMapper.countLink",link);
            return count != 0;
        }
    }

    @Override
    public void insertLinkIntoToBeProcessed(String href) throws SQLException {
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "links_to_be_processed");
        param.put("link", href);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.Hana.MyMapper.insertLink", param);
        }
    }
}
