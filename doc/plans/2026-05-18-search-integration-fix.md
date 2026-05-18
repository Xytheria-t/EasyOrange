# 搜索功能前后端数据链路修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复搜索分面（facet）数据丢失的前后端数据链路，使 `FacetFilter` 能正常渲染 ES 聚合数据

**Architecture:** 后端新增 `SearchPageResponse` 和 `FacetBucketResponse` DTO，在 `ProductSearchHandler` 中合并三组 facet 并前缀分组名；前端修复价格区间标签格式化函数

**Tech Stack:** Java 25 / Spring Boot 4 / TypeScript / React

---

### Task 1: 新建后端 DTO 文件

**Files:**
- Create: `easyorange-backend/easyorange-product/src/main/java/com/cartethyia/easyorange/product/adapter/inbound/web/dto/response/FacetBucketResponse.java`
- Create: `easyorange-backend/easyorange-product/src/main/java/com/cartethyia/easyorange/product/adapter/inbound/web/dto/response/SearchPageResponse.java`

- [ ] **Step 1: 创建 FacetBucketResponse**

```java
package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

public record FacetBucketResponse(String code, long count) {}
```

- [ ] **Step 2: 创建 SearchPageResponse**

```java
package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import java.util.List;

public record SearchPageResponse<T>(
    List<T> records,
    long total,
    int pageNum,
    int pageSize,
    List<FacetBucketResponse> facets
) {
    public static <T> SearchPageResponse<T> of(List<T> records, long total, int pageNum, int pageSize) {
        return new SearchPageResponse<>(
            records != null ? records : List.of(),
            total,
            pageNum,
            pageSize,
            List.of()
        );
    }

    public static <T> SearchPageResponse<T> of(List<T> records, long total, int pageNum, int pageSize,
                                               List<FacetBucketResponse> facets) {
        return new SearchPageResponse<>(
            records != null ? records : List.of(),
            total,
            pageNum,
            pageSize,
            facets != null ? List.copyOf(facets) : List.of()
        );
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd /home/cartethyia/projects/Java/easy-orange/easyorange-backend
./mvnw compile -pl easyorange-product -q
```

Expected: BUILD SUCCESS

---

### Task 2: 修改 ProductSearchHandler

**Files:**
- Modify: `easyorange-backend/easyorange-product/src/main/java/com/cartethyia/easyorange/product/application/query/handler/ProductSearchHandler.java`

- [ ] **Step 1: 添加新 import**

在 `ProductSearchHandler.java` 文件顶部的 import 块中添加：

```java
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.FacetBucketResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchPageResponse;
import java.util.ArrayList;
```

- [ ] **Step 2: 修改 handleSearch 返回类型和 ES/MySQL 路径**

将第 36 行的返回类型 `PageResult<ProductResponse>` 改为 `SearchPageResponse<ProductResponse>`。

替换第 49-53 行（ES 路径的返回语句）为 facets 合并版本。
替换第 63-68 行（MySQL 降级路径的返回语句）为 `SearchPageResponse.of()`。

修改后完整方法内容：

```java
@Transactional(readOnly = true)
public SearchPageResponse<ProductResponse> handleSearch(ProductSearchRequest request) {
    if (searchQueryPort.isPresent()) {
        ProductSearchQueryPort.ProductSearchQuery query = new ProductSearchQueryPort.ProductSearchQuery(
                request.getKeyword(),
                request.getCategoryId(),
                request.getStatus(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getConditionLevel(),
                request.getSortField(),
                request.getPageNum() != null ? request.getPageNum() : 1,
                request.getPageSize() != null ? request.getPageSize() : 20
        );
        SearchResult searchResult = searchQueryPort.get().search(query);
        List<ProductResponse> responses = searchResult.records().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
        List<FacetBucketResponse> facets = mergeFacets(searchResult);
        return SearchPageResponse.of(responses, searchResult.total(), searchResult.pageNum(),
                searchResult.pageSize(), facets);
    }

    Page<ProductReadModel> page = productQueryRepository.searchProducts(
            request.getKeyword(),
            request.getCategoryId(),
            request.getStatus(),
            request.getPageNum() != null ? request.getPageNum() : 1,
            request.getPageSize() != null ? request.getPageSize() : 20
    );

    List<ProductResponse> responses = page.getRecords().stream()
            .map(this::toProductResponse)
            .collect(Collectors.toList());

    return SearchPageResponse.of(responses, page.getTotal(), (int) page.getCurrent(),
            (int) page.getSize());
}
```

- [ ] **Step 3: 添加 mergeFacets 私有方法**

在 `toProductResponse` 方法（第 117 行附近）之后添加：

```java
private List<FacetBucketResponse> mergeFacets(SearchResult result) {
    var list = new ArrayList<FacetBucketResponse>();
    result.categoryFacets().forEach(fb ->
        list.add(new FacetBucketResponse("category_" + fb.key(), fb.count())));
    result.conditionFacets().forEach(fb ->
        list.add(new FacetBucketResponse("condition_" + fb.key(), fb.count())));
    result.priceRangeFacets().forEach(fb ->
        list.add(new FacetBucketResponse("price_" + fb.key(), fb.count())));
    return List.copyOf(list);
}
```

- [ ] **Step 4: 编译验证**

```bash
cd /home/cartethyia/projects/Java/easy-orange/easyorange-backend
./mvnw compile -pl easyorange-product -q
```

Expected: BUILD SUCCESS

---

### Task 3: 修改 SearchController

**Files:**
- Modify: `easyorange-backend/easyorange-product/src/main/java/com/cartethyia/easyorange/product/adapter/inbound/web/SearchController.java`

- [ ] **Step 1: 修改 searchProducts 返回类型**

将第 34-36 行的：
```java
@GetMapping
public Result<PageResult<ProductResponse>> searchProducts(@Valid ProductSearchRequest request) {
    PageResult<ProductResponse> result = searchHandler.handleSearch(request);
```
改为：
```java
@GetMapping
public Result<SearchPageResponse<ProductResponse>> searchProducts(@Valid ProductSearchRequest request) {
    SearchPageResponse<ProductResponse> result = searchHandler.handleSearch(request);
```

- [ ] **Step 2: 编译验证**

```bash
cd /home/cartethyia/projects/Java/easy-orange/easyorange-backend
./mvnw compile -pl easyorange-product -q
```

Expected: EOF in String literal</token>
```

Expected: BUILD SUCCESS

---

### Task 4: 运行测试验证

**Files:**
- (no changes)

- [ ] **Step 1: 运行 ProductSearchHandlerTest**

```bash
cd /home/cartethyia/projects/Java/easy-orange/easyorange-backend
./mvnw test -pl easyorange-product -Dtest=ProductSearchHandlerTest
```

Expected: Tests pass (BUILD SUCCESS)

If tests fail, check:
- Mock return types may need updating from `PageResult` to `SearchPageResponse`
- `Result.success()` expects different type parameter

---

### Task 5: 前端 FacetFilter 修复

**Files:**
- Modify: `easyorange-frontend/src/components/search/FacetFilter.tsx`

- [ ] **Step 1: 替换 formatPriceLabel 函数**

将第 57-65 行的 `formatPriceLabel` 函数体替换为：

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

- [ ] **Step 2: TypeScript 类型检查**

```bash
cd /home/cartethyia/projects/Java/easy-orange/easyorange-frontend
npx tsc --noEmit --pretty 2>&1 | head -50
```

Expected: No type errors related to FacetFilter or product types

---

### Task 6: 全量编译验证

- [ ] **Step 1: 后端全量编译**

```bash
cd /home/cartethyia/projects/Java/easy-orange/easyorange-backend
./mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 前端构建**

```bash
cd /home/cartethyia/projects/Java/easy-orange/easyorange-frontend
npm run build 2>&1 | tail -20
```

Expected: Build completes without errors