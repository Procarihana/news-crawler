package com.github.Hana;

import org.apache.http.HttpHost;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticsearchDataGenerator {
    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory;
        try {
            String resource = "db/Mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory =
                    new SqlSessionFactoryBuilder().build(inputStream);
        } catch (
                Exception e) {
            throw new RuntimeException(e);
        }

        List<MockNews> newsfromMocData = getDatafromMockDatabase(sqlSessionFactory);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> writeSingleThread(newsfromMocData)).start();
        }
    }

    private static void writeSingleThread(List<MockNews> newsfromMocData) {
        for (int i = 0; i < 2000; i++) {


            try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
                IndexRequest request = new IndexRequest("news");
                for (MockNews news : newsfromMocData) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", news.getTitle());
                    data.put("content", news.getContent());
                    data.put("url", news.getUrl());
                    data.put("createdAt", news.getCreatedAt());
                    data.put("modifiedAt", news.getModifiedAt());
                    request.source(data, XContentType.JSON);
                    IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
                    System.out.println(indexResponse.status().getStatus());
                    BulkRequest bulkRequest = new BulkRequest();
                    bulkRequest.add(request);
                    BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                    System.out.println("Current Thread:" + Thread.currentThread().getName() + "finishes" + bulkResponse.status().getStatus());
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static List<MockNews> getDatafromMockDatabase(SqlSessionFactory sqlSessionFactory) {
        try (
                SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList("com.github.Hana.MockMapper.selectNews");
        }
    }
}
