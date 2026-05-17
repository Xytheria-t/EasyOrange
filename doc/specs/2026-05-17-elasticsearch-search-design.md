# ElasticSearch 全文搜索集成设计

> 日期: 2026-05-17
> 状态: 设计稿

## 1. 目标

将当前基于 MySQL ngram FULLTEXT 的商品搜索升级为 ElasticSearch，提升搜索体验。

### 能力提升

- **模糊搜索**: fuzziness AUTO 容错（"iphon" → "iPhone"）
- **中文分词**: IK 插件 ik_max_word / ik_smart
- **相关度调优**: name 字段权重 x3，description 权重 x1
- **分面聚合**: 一次查询同时返回分类/成色/价格区间的计数统计
- **性能**: 搜索不扫描 MySQL 表，完全由 ES 承载

## 2. 架构

### 核心原则

- **零侵入现有 DDD 分层**: 沿用 Port/Adapter + 事件驱动模式
- **CQRS 严格分离**: 写路径通过事件同步索引，读路径通过 ES 端口查询
- **无需修改 product 模块核心逻辑**, 仅在 application 模块增加 ES 适配实现

### 写路径（索引同步）— 替换现有适配器

```
Product 聚合根 → 领域事件 (ProductCreated/Updated/Deleted/MarkedSold)
  → ProductEventListener (已有, @Async + AFTER_COMMIT)
    → ProductSearchIndexPort (已有接口, product.domain.port.output)
      → ElasticsearchProductSearchIndexAdapter (新增, application 模块)
            替代: ProductSearchIndexAdapter (写入 MySQL search_text)
```

### 读路径（搜索查询）— 新增端口

```
SearchController → ProductSearchHandler
  → 优先: ProductSearchQueryPort → ElasticsearchProductSearchQueryAdapter
  → 降级: ProductQueryRepository.searchProducts() (MySQL FULLTEXT)
```

### 模块变更清单

| 模块 | 变更 | 说明 |
|------|------|------|
| `product` | 新增 `ProductSearchQueryPort` + `SearchResult` + `FacetBucket` 接口 (`domain/port/output/`) | 定义搜索契约 |
| `product` | `ProductSearchHandler` 注入 `Optional<ProductSearchQueryPort>`，ES 可用→走 ES，否则→MySQL | 路由决策在应用层 |
| `application` | 新增 2 个 adapter + 1 个 reindex service | 主要实现 |
| `application` | pom.xml 新增 `spring-boot-starter-data-elasticsearch` | Maven 依赖 |
| `frontend` | 搜索响应补充 `aggregations` 类型定义 | 前端适配 |

## 3. Index Mapping

### 索引管理方式

JSON 文件 + 编程式创建，避免 Spring Data 注解对复杂分析器配置的限制。

文件位置: `src/main/resources/elasticsearch/product-settings.json` 和 `product-mapping.json`

创建时机: `@EventListener(ApplicationReadyEvent.class)` 中 `IndexOperations.create()` + `putMapping()`

### Settings

```json
{
  "number_of_shards": 1,
  "number_of_replicas": 1
}
```

IK 插件全局注册 `ik_max_word` / `ik_smart` 分析器，无需额外声明。

### Mapping

```json
{
  "properties": {
    "id":              { "type": "keyword" },
    "userId":          { "type": "keyword" },
    "name": {
      "type": "text",
      "analyzer": "ik_max_word",
      "search_analyzer": "ik_smart",
      "fields": { "keyword": { "type": "keyword" } }
    },
    "description": {
      "type": "text",
      "analyzer": "ik_max_word",
      "search_analyzer": "ik_smart"
    },
    "categoryId":      { "type": "integer" },
    "categoryName":    { "type": "keyword" },
    "price":           { "type": "scaled_float", "scaling_factor": 100 },
    "originalPrice":   { "type": "scaled_float", "scaling_factor": 100 },
    "conditionLevel":  { "type": "byte" },
    "status":          { "type": "byte" },
    "viewCount":       { "type": "integer" },
    "stock":           { "type": "integer" },
    "location":        { "type": "keyword" },
    "tags":            { "type": "keyword" },
    "mainImage":       { "type": "keyword" },
    "images":          { "type": "keyword" },
    "sellerName":      { "type": "keyword" },
    "sellerAvatar":    { "type": "keyword" },
    "createTime":      { "type": "date", "format": "yyyy-MM-dd HH:mm:ss||epoch_millis" },
    "updateTime":      { "type": "date", "format": "yyyy-MM-dd HH:mm:ss||epoch_millis" }
  }
}
```

### 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| **中文分词** | IK 插件 | 二手商品标题混合中英文+数字，切分精度远优于内置 cjk |
| **price 类型** | scaled_float (×100) | 精确存储分位，ES 内部存 long，比 double 节省 40% 空间 |
| **name 双字段** | text + .keyword 子字段 | 分词搜索 + 精确聚合 |
| **反范式化** | 展平存到 ES | 搜索结果卡片免 JOIN，零 N+1 |
| **tags** | keyword 数组 | 替代 MySQL 逗号字符串，支持精确匹配 |

## 4. Java 数据模型

### Document 类

```java
@Document(indexName = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {
    @Id
    private String id;
    private Long userId;
    private String name;
    private String description;
    private Integer categoryId;
    private String categoryName;
    private Double price;
    private Double originalPrice;
    private Byte conditionLevel;
    private Byte status;
    private Integer viewCount;
    private Integer stock;
    private String location;
    private List<String> tags;
    private String mainImage;
    private List<String> images;
    private String sellerName;
    private String sellerAvatar;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 读端口接口（product 模块 domain 层）

```java
// 端口定义
public interface ProductSearchQueryPort extends OutboundPort {
    SearchResult search(ProductSearchQuery query);
}

// 查询入参
@Value @Builder
public class ProductSearchQuery {
    String keyword;
    Long categoryId;
    BigDecimal minPrice, maxPrice;
    Byte conditionLevel;
    String sort;          // relevance / price_asc / price_desc / newest / popular
    int pageNum, pageSize;
}

// 查询结果（含分面）
@Value @Builder
public class SearchResult {
    List<ProductReadModel> records;   // 复用已有 ReadModel
    long total;
    int pageNum, pageSize;
    List<FacetBucket> categoryFacets;
    List<FacetBucket> conditionFacets;
    List<FacetBucket> priceRangeFacets;
}

// 分面桶
@Value @Builder
public class FacetBucket {
    String key;
    String label;
    long count;
}
```

> ES Adapter 内部将 `ProductDocument` → `ProductReadModel` 转换后返回，domain 层不感知 ES 类型。

## 5. 搜索查询设计

### ES Query DSL

```json
{
  "query": {
    "bool": {
      "must": [
        { "multi_match": {
            "query": "iPhone",
            "fields": ["name^3", "description"],
            "type": "best_fields",
            "fuzziness": "AUTO"
        }}
      ],
      "filter": [
        { "term": { "status": 1 } },
        { "term": { "categoryId": 10 } },
        { "range": { "price": { "gte": 100, "lte": 5000 } } }
      ]
    }
  },
  "sort": [
    { "_score": "desc" },
    { "createTime": "desc" }
  ],
  "aggs": {
    "category":        { "terms": { "field": "categoryId", "size": 20 }},
    "conditionLevel":  { "terms": { "field": "conditionLevel" }},
    "priceRanges":     { "range": { "field": "price", "ranges": [
      { "to": 100 }, { "from": 100, "to": 500 }, { "from": 500, "to": 1000 }, { "from": 1000 }
    ]}}
  }
}
```

### 搜索能力对照

| 能力 | MySQL FULLTEXT | ES |
|------|---------------|----|
| 模糊搜索 | ❌ 不支持 | ✅ fuzziness AUTO |
| 中文分词 | ngram (字粒度) | IK 词粒度 |
| 字段权重 | 无法调优 | name^3 / description^1 |
| 分面聚合 | 需独立 SQL COUNT | 一次查询返回 |
| 搜索建议 | DB LIKE 扫描 | prefix/suggester |
| 排序灵活性 | 有限 | 多字段+评分组合 |

## 6. 数据同步

### 增量同步

复用 `ProductEventListener` 中已有的 `ProductSearchIndexPort` 调用链：

| 事件 | 方法 | 触发时机 |
|------|------|---------|
| ProductCreated | indexProduct(id) | 商品创建并提交后 |
| ProductUpdated | updateProductIndex(id) | 商品信息修改后 |
| ProductDeleted | removeProductIndex(id) | 商品删除后 |
| ProductMarkedSold | updateProductIndex(id) | 标记售出后 |
| StockRestored | updateProductIndex(id) | 库存恢复后 |

### 全量重建

Admin API: `POST /api/admin/search/reindex`

启动自动重建: `ApplicationReadyEvent` 监听，检测 ES 索引为空时触发。

重建流程：分页读取 MySQL 在线商品 → `buildDocument()` 组装 → `operations.saveAll()` 批量写入。

## 7. 部署

### docker-compose

```yaml
elasticsearch:
  build: ./docker/elasticsearch     # Dockerfile 预装 IK 插件
  container_name: easyorange-es
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
    - ES_JAVA_OPTS=-Xms512m -Xmx512m
  ports:
    - "9200:9200"
  volumes:
    - es-data:/usr/share/elasticsearch/data
  deploy:
    resources:
      limits:
        memory: 1G

kibana:                              # 开发环境可选
  image: kibana:8.17.3
  profiles: ["dev"]
```

### 依赖

`easyorange-application/pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

### 配置

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    connection-timeout: 5s
    socket-timeout: 30s
```

## 8. 测试策略

| 层次 | 方式 | 覆盖 |
|------|------|------|
| 单元测试 | Mock ElasticsearchOperations | Adapter 查询构建逻辑 |
| 集成测试 | Testcontainers ES | 中文搜索、分面聚合、精确/模糊匹配 |
| 全链路 | Testcontainers MySQL + ES | 事件→索引同步完整性 |

现有 `ProductEventListener` 测试和 `ProductSearchHandler` 测试无需修改。

## 9. 未纳入范围

以下能力不在本设计范围内，可作为后续迭代：

- **搜索建议 (suggest)**: 搜索框前缀补全，当前通过 Redis ZSET + MySQL 实现，可用 ES Completion Suggester 增强
- **同义词**: 配置 IK 同义词词典（如"手机"="电话"）
- **语义搜索**: 结合嵌入向量的向量搜索
- **图片搜索**: 以图搜图
