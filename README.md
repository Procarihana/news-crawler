## 多线程网络爬虫+新闻搜索引擎
这是一个利用 HttpClient + jsoup 对新闻网站进行搜索,通过 Elasticsearch 建立新闻引擎的后端项目。
爬虫模块对新闻网站进行广度优先搜索，爬取HTML并进行解析。爬取的数据通过接口方式存入数据库，能够通过配置无缝切换不同的读写方式（JDBC/MyBatis）和后端数据存储服务（H2/MySQL/Elasticsearch）。

新闻搜索引擎使用MySQL和Elasticsearch进行新闻的搜索。当爬取到的数据在百万规模时，通过MySQL索引使得数据查找时间下降了近百倍；
当爬取到的数据在千万规模时，通过Elasticsearch全文索引，控制文本搜索时间在毫秒级。

- 爬取网站: https://sina.cn/
- - 该网站没有设置反爬虫机制，且手机页面网站设置比较简单，更适合练习爬虫。
## 项目结构
- `Service`: 处理爬虫和搜索引擎业务逻辑的方法实现，依赖于 Dao 层的数据库操作。
- `Dao`: 提供实现访问数据库的方法，通过 MyBatis 完成和数据库的交互。
- `Entity`: 用于存放实体类。
- `Generator`: 用于实现 Elasticsearch 添加数据的业务。
## 重现
#### clone 项目到本地
`git clone https://github.com/Procarihana/news-crawler.git`
#### Docker 启动 数据库
- 启动MySQL数据库
`docker run --name springboot-mysql -e MYSQL_ROOT_PASSWORD=hana -e -p 3306:3306 -d mysql`
- Docker 使用注意事项
1. 需要外部访问数据库时，要进行 `-p`配置。
2. 数据库持久化需要进行 `-v`配置。
3. 远程访问数据库时，需要先通过 Docker 进入到 MySql 容器里对用户可登陆 IP 地址进行更改。
#### 使用 Flyway 进行数据库初始化
- 数据库新建以及 Maven 刷新后，可执行 Flyway 对数据库进行建表自动建表操作。
`mvn flyway:migrate`
- 注意：数据库里面存储的是 `https://sina.cn/` 作为原始爬取的网站，如果想要爬取其他网站的需要自行放入。
- 如若需要，可以直接从newsdata.sql中直接获取数据。
#### Docker 启动 Elasticsearch
- (参考文档)[https://www.elastic.co/guide/en/elasticsearch/reference/current/docker.html]
`docker run -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" --name elastic docker.elastic.co/elasticsearch/elasticsearch:7.9.2`
- - Docker 的 Elasticsearch 镜像最好是选择实例（官方文档推荐），否则直接使用最新版本时，数据的添加有可能会出现异常。
- - 如果 Docker 配置容器的内存不足够运行 Elasticsearch 容器，就会出现闪退和无法访问的情况。可在新建容器的时候添加参数 `-e ES_JAVA_OPTS="-Xms256m -Xmx256m" `来解决。
- 容器启动后马上访问不一定能够访问成功，需要等待一下。
- - 测试容器启动：http://localhost:9200
#### 项目测试
- `mvn verify`
#### 项目运行
1. 运行爬虫爬取网页信息
- 运行 CrawlerMain 类
- - 需要大量信息时，可直接运行 MockDataGenerator 类的 main 方法，通过修改 howmany 参数自动定义需要获得数据的数量。
2. 运行 Elasticsearch 搜索引擎
- 运行 ElasticsearchDataGenerator 类的 main 方法把数据库里的数据添加到搜索引擎里。
- 访问 http://localhost:9200/news/_search?q=title: +关键字 
3. 直接运行 ElasticsearchHighLineSearchMain 类并输入关键字亦可。

