# 搜索功能前后端数据链路修复

## 背景

ES 后端适配器（`ElasticsearchProductSearchQueryAdapter`）已实现，能返回分面聚合（category/condition/price range）结果，但 `ProductSearchHandler` 在组装响应时丢弃了 facet 数据。前端 `FacetFilter` 组件已实现但从未接收到实际的 facet 数据。

## 问题清单

| # | 问题 | 严重度 | 描述 |
|---|------|--------|------|
| 1 | Facet 数据丢失 | 🔴 P0 | `ProductSearchHandler` 将 `SearchResult` 转 `PageResult` 时丢弃 `categoryFacets`/`conditionFacets`/`priceRangeFacets` |
| 2 | 响应结构不匹配 | 🟡 P1 | 后端返回 `PageResult`（字段: records/total/current/size/pages），前端 `ProductSearchResult` 期望 `{records, total, pageNum, pageSize, facets}` |
| 3 | `SearchResultResponse` 未使用 | 🟢 P3 | 存在但未被任何代码引用的 record 类 |

## 解决方案

新增搜索专用 DTO `SearchPageResponse` 替代通用 `PageResult`，封装 facets 数据。

### 数据流（修复后）

```
ES → ElasticsearchProductSearchQueryAdapter
     → SearchResult { records, total, pageNum, pageSize, categoryFacets, conditionFacets, priceRangeFacets }

ProductSearchHandler
     → 合并三组 facet，前缀分组名
     → SearchPageResponse { records, total, pageNum, pageSize, facets: [{code, count}, ...] }

SearchController → JSON → { code, message, data: { records, total, pageNum, pageSize, facets } }

前端 useSearch → searchResult.facets → FacetFilter 渲染
```

### 后端改动

#### 1. 新建 `FacetBucketResponse`

- **位置**: `easyorange-product/adapter/inbound/web/dto/response/FacetBucketResponse.java`
- **内容**: `public record FacetBucketResponse(String code, long count) {}`
- **字段命名**: 使用 `code`（匹配前端 `FacetBucket.code`），而非后端 `FacetBucket.key`

#### 2. 新建 `SearchPageResponse<T>`

- **位置**: `easyorange-product/adapter/inbound/web/dto/response/SearchPageResponse.java`
- **字段**: `records`, `total`, `pageNum`, `pageSize`, `facets`
- **静态工厂**: `of(records, total, pageNum, pageSize)` — 默认空 facets；`of(records, total, pageNum, pageSize, facets)` — 带 facets
- **不可变**: 使用 `List.of()` 和 `List.copyOf()`

#### 3. 修改 `ProductSearchHandler`

- 返回类型从 `PageResult<ProductResponse>` 改为 `SearchPageResponse<ProductResponse>`
- ES 路径: 新增 `mergeFacets(SearchResult)` 方法，合并三组 facet，分组名前缀:
  - `categoryFacets` → `"category_" + key`
  - `conditionFacets` → `"condition_" + key`
  - `priceRangeFacets` → `"price_" + key`
- MySQL 降级路径: 使用 `SearchPageResponse.of()` 无 facets 版本

#### 4. 修改 `SearchController`

- `searchProducts()` 返回类型改为 `Result<SearchPageResponse<ProductResponse>>`

### 前端改动

#### 5. 修改 `FacetFilter.tsx` — `formatPriceLabel`

当前 `formatPriceLabel` 按 `_` 分割，但 ES 价格区间 key 格式为 `"*-100"`、`"100-500"` 等（`-` 分隔）。改为按 `-` 分割并处理 `*` 通配符:

```typescript
function formatPriceLabel(value: string): string {
  const parts = value.split('-');
  if (parts.length === 2) {
    const [min, max] = parts;
    if (min === '*' && max) return `¥0 - ¥${max}`;
    if (min && max === '*') return `¥${min}+`;
    if (min && max) return `¥${min} - ¥${max}`;
  }
  return value;
}
```

#### 6. `SearchPage.tsx` — 无需改动

## 改动清单

| # | 文件 | 操作 | 行数 |
|---|------|------|------|
| 1 | `dto/response/FacetBucketResponse.java` | 新建 | ~5 |
| 2 | `dto/response/SearchPageResponse.java` | 新建 | ~30 |
| 3 | `application/query/handler/ProductSearchHandler.java` | 修改 | +10 |
| 4 | `adapter/inbound/web/SearchController.java` | 修改 | +1 |
| 5 | `FacetFilter.tsx` | 修改 | 替换函数体 |
| 6 | `SearchPage.tsx` | 无需改动 | — |

## 未涵盖的内容

- 分页状态管理（前端 `pageNum`/`pageSize` 管理已在 `SearchPage` 中通过 `useMemo` 实现，无需额外改动）
- 排序功能（`sort` 参数已在 `ProductSearchQueryPort` 和 `useSearch` 中传递，ES 侧已实现）
- 搜索结果历史的后端 API（前端使用 localStorage，后端 API 存在但未被前端集成——此为独立功能，非本次范围）

## 验证策略

1. **编译验证**: 后端 `./mvnw compile -pl easyorange-product`；前端 `npm run typecheck`
2. **单元测试**: `./mvnw test -pl easyorange-product -Dtest=ProductSearchHandlerTest`
3. **数据流验证**: 检查 `SearchPageResponse` JSON 输出包含 `facets` 数组，各 facet 的 `code` 格式为 `{group}_{value}`，`count` 为正确计数值