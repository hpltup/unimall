# unimall-service-search 搜索模块说明

> 搜索服务：Elasticsearch 全文检索（**IK 中文 + 拼音**分词），商品数据定时同步。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、索引设计（IK + 拼音）](#三索引设计ik--拼音)
- [四、接口清单](#四接口清单)
- [五、功能实现思路](#五功能实现思路)
- [六、目录结构](#六目录结构)
- [七、配置说明](#七配置说明)
- [八、启动前提与已知问题](#八启动前提与已知问题)

---

## 一、模块定位

搜索服务（端口 **10020**）：商品全文检索（中文分词 + 拼音容错），数据从 goods 服务**定时全量同步**到 ES。

## 二、技术栈

Spring Boot 3.3.7 / Spring Cloud 2023.0.1 / Nacos / Config / **Spring Data Elasticsearch 5.3.x**（支持 ES 8.x）/ OpenFeign（同步拉数据）。

**无 MySQL / MyBatis-Plus**（纯 ES 服务）。

## 三、索引设计（IK + 拼音）

- `GoodsDoc`（`@Document(indexName="goods")` + `@Setting` + `@Mapping`）
- **settings**（`es/goods-settings.json`）：自定义 analyzer `ik_pinyin_analyzer` = `ik_max_word` + `pinyin` filter（保留全拼/首字母/原文）
- **mapping**（`es/goods-mapping.json`）：`name` / `subTitle` 用 `ik_pinyin_analyzer` 索引、`ik_smart` 搜索（中文原词优先，避免拼音干扰相关性）

## 四、接口清单

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `GET /search/goods?keyword=&pageNum&pageSize=` | 全文检索：`multiMatch` 查 name/subTitle + 过滤 status=1 + **按销量降序** | **白名单**（`/api/search/goods`） |
| `POST /search/sync` | 手动触发全量同步（管理） | 需登录 |

## 五、功能实现思路

### 数据同步（goods → ES）

```
定时 @Scheduled(fixedDelay = 600000)（10 分钟）+ 手动 POST /search/sync
  → goodsClient.allOnSale() 调 goods /goods/internal/for-search（全部上架商品）
  → GoodsVO 转 GoodsDoc → operations.save（按 id 覆盖，幂等）
  → goods 服务不可用时跳过并 warn 日志
```

### 搜索

```
search(keyword, pageNum, pageSize)
  → NativeQuery：bool(must: multiMatch("name","subTitle") + filter: term(status=1))
  → sort: sales 降序 → PageRequest 分页
  → operations.search → SearchHits<GoodsDoc> → 转 SearchPageVO<GoodsVO>
```

返回 `SearchPageVO`（records/total/current/size，**字段名兼容 MyBatis-Plus Page**，前端与其他服务无感）。

## 六、目录结构

```
com.unimall.search/
├── SearchApplication.java         # @SpringBootApplication + @EnableFeignClients + @EnableScheduling
├── controller/SearchController.java
├── service/ISearchService.java + impl/SearchServiceImpl.java
├── client/IGoodsClient.java
├── repository/GoodsDocRepository.java
├── pojo/doc/GoodsDoc.java、pojo/vo/SearchPageVO.java
└── config/GlobalExceptionHandler.java

resources/es/goods-settings.json + goods-mapping.json
```

## 七、配置说明

- 本地 `application.yml`：端口 10020、Nacos、config 客户端
- 配置中心 `config-repo/search-dev.yml`：`spring.elasticsearch.uris: http://192.168.89.101:9200`（ES 在虚拟机；若开安全认证需补 username/password）

## 八、启动前提与已知问题

- 前提：**ES 虚拟机启动 + IK/拼音插件已装**（已验证：中文"华为"+拼音"huawei"均命中）、Nacos、config 启动（git 模式）、goods 服务启动（同步依赖）
- 实现要点：`GoodsDoc.createTime` 用 **`Long`（epoch millis）**（Spring Data ES 对 LocalDateTime 存 Long 后无法读回，同步/返回手动转换）；`stock` 字段已映射
- 已知问题：全量同步（10 分钟）够学习用，数据量大需 canal 增量/双写；未做搜索高亮/价格过滤/聚合分面；`GoodsDoc.categoryName` 预留但 goods 未提供（暂为 null）
