# unimall-service-search — 搜索服务

搜索服务（端口 **10020**）：Elasticsearch 全文检索（**IK 中文 + 拼音**分词），商品数据定时同步。

## 接口

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `GET /search/goods?keyword=&pageNum=&pageSize=` | 全文检索：`multiMatch` 查 name/subTitle（IK+拼音），过滤 status=1，**按销量降序** | **公开**（白名单 `/api/search/goods`） |
| `POST /search/sync` | 手动触发全量同步（管理接口） | 需登录 |

## 核心设计

### 数据同步（goods → ES）
- goods 内部接口 `GET /goods/internal/for-search`（全部上架商品，`List<GoodsVO>`）
- search Feign `IGoodsClient` 拉取 → `ElasticsearchOperations.save` 全量写入（按 id 覆盖，幂等）
- **定时任务**：`@EnableScheduling` + `@Scheduled(fixedDelay = 600000)`（10 分钟），另有手动触发接口

### 索引 goods（IK + 拼音）
- `GoodsDoc`：`@Document(indexName="goods")` + `@Setting(settingPath)` + `@Mapping(mappingPath)`
- `es/goods-settings.json`：自定义 analyzer `ik_pinyin_analyzer`（`ik_max_word` + `pinyin` filter，保留全拼/首字母）
- `es/goods-mapping.json`：`name`/`subTitle` 用 `ik_pinyin_analyzer` 索引、`ik_smart` 搜索（中文原词优先）

## 分层

```
com.unimall.search/
├── SearchApplication.java        # @EnableFeignClients + @EnableScheduling
├── controller/SearchController.java   # /search/goods、/search/sync
├── service/ISearchService.java + impl/SearchServiceImpl.java
├── client/IGoodsClient.java      # Feign → goods /goods/internal/for-search
├── repository/GoodsDocRepository.java
├── pojo/doc/GoodsDoc.java        # ES 文档
├── pojo/vo/SearchPageVO.java     # 分页（字段名兼容 MyBatis-Plus Page）
└── config/GlobalExceptionHandler.java

resources/es/goods-settings.json + goods-mapping.json
```

## 要点

- **无 MySQL / MyBatis-Plus**：纯 ES 服务，不落库
- ES 配置：`spring.elasticsearch.uris: http://192.168.89.101:9200`（配置中心 `search-dev.yml`；**已验证**：中文"华为" + 拼音"huawei"均命中；若开安全认证需补 username/password）
- **`GoodsDoc.createTime` 用 `Long`（epoch millis）**：Spring Data ES 对 LocalDateTime 存成 Long 后无法读回（曾报 ConverterNotFoundException），同步/返回时手动转换；`stock` 字段已映射
- 查询用 `ElasticsearchOperations` + `NativeQueryBuilder`（Spring Data ES 5.3.x / ES Java Client 8.x DSL）
- 返回 `SearchPageVO<GoodsVO>`（records/total/current/size，前端与其他服务 MP Page 无感）
- 搜索/同步依赖 goods 服务（Feign），goods 挂了同步会跳过（warn 日志）

## 已知问题/后续

- 全量同步（10 分钟）够学习用，数据量大需 canal 增量 / 双写
- 未做搜索高亮、价格区间过滤、聚合分面
- `GoodsDoc.categoryName` 预留但 goods 未提供（`GoodsVO` 无该字段，暂为 null）
