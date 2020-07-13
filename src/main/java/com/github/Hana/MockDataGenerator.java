package com.github.Hana;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class MockDataGenerator {
    private static void mockData(SqlSessionFactory sqlSessionFactory, int howMany) {

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            List<MockNews> currentNews = session.selectList(
                    "com.github.Hana.MockMapper.selectNews");
            int count = howMany - currentNews.size();
            Random random = new Random();
            try {
                while (count-- > 0) {
                    int index = random.nextInt(currentNews.size());  //random生成的伪随机数是从0到指定的数值-1
                    MockNews newsToBeInsert = currentNews.get(index);

                    Instant currentTime = newsToBeInsert.getCreatedAt();
                    currentTime = currentTime.minusSeconds(random.nextInt(3600 * 24 * 365));  //改时间戳，随机减去一年里面的一个时间
                    newsToBeInsert.setCreatedAt(currentTime);
                    session.insert("com.github.Hana.MockMapper.insertNews", newsToBeInsert);

                }
                session.commit();//执行完所有的操作正确完成后，提交，如果出现问题就能够回滚。
            } catch (Exception e) {
                session.rollback(); //回滚
                throw new RuntimeException(e);

            }

        }
    }


    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory;
        try {
            String resource = "db/Mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory =
                    new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mockData(sqlSessionFactory, 1000);
    }

}


