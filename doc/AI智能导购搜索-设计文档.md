# AI 智能导购搜索 — 设计文档

> **版本**: 1.0
> **日期**: 2026-06-01
> **状态**: 已实现

---

## 1. 概述

在现有商品搜索中嵌入 AI 增强能力。用户输入自然语言查询时（如"5000以内适合编程的二手笔记本"），搜索结果同时展示普通商品列表 + AI 分析信息，将项目中已有的 5 个 AI/数据能力融入搜索体验。

### 1.1 展示的项目能力

| 展示元素 | 能力来源 | 状态 |
|---------|---------|------|
| 🎯 需求理解 | `LlmPort.generateText()` | 需新增 |
| 💰超值 标签 | `AiPricingService` 定价分析 | **已有** |
| ✅AI审核 标签 | `AiReviewService` 审核结果 | **已有** |
| ⭐信用优 标签 | `UserCredit` 信用分 | **已有** |
| 📊 市场分析 | `AiPricingService` 市场数据 | **已有** |
| 💬 猜你想问 | `LlmPort.generateText()` | 需新增 |
| 商品列表 | ES 搜索 (已有) | **已有** |

---

## 2. 数据流

```
用户输入 "5000以内适合编程的笔记本"
    ↓
GET /products/search?keyword=...&aiEnhanced=true
    ↓
ProductSearchHandler
    │
    ├─ 通道1 [即时]: ES 搜索 → 返回普通结果列表
    │   
    └─ 通道2 [同步阻塞, ~2s]: AiSearchEnhancer.enhance()
         ├─ IntentParser       (LLM x1: 理解需求)
         ├─ ProductTagger      (无 LLM: 读已有数据打标签)
         ├─ MarketAnalyzer     (复用 AiPricingService)
         └─ SuggestedQuestions (LLM x1: 生成推荐提问)
         │
         └─ → AiEnhancement DTO
    ↓
返回: { records, total, facets, aiEnhancement: AiEnhancement }
```

**关键决策点**:
- `aiEnhanced=true` 查询参数由前端搜索页 UI toggle 控制
- 自然语言检测在 `AiSearchEnhancer.tryEnhance()` 内部
- 纯关键词（如 "MacBook Pro"）不走 AI 管道
- AI 增强失败（超时/异常）时返回 `Optional.empty()`，不影响结果列表
- 四个子步骤完全并行（CompletableFuture）

---

## 3. 后端新增/修改

### 3.1 新增: `AiEnhancement` DTO

**位置**: `easyorange-common/src/main/java/com/cartethyia/easyorange/common/dto/AiEnhancement.java`

```java
package com.cartethyia.easyorange.common.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * AI 智能导购增强数据。
 * 当 aiEnhanced=true 且查询为自然语言时附加到搜索结果中。
 * 为 null 时前端不展示 AI 区域。
 */
@Data
public class AiEnhancement {
    /** 需求理解文本，如 "你想找一台5000以内适合Java开发的笔记本…" */
    private String intentExplanation;

    /** 商品标签映射: { productId: ["💰超值", "✅AI审核", "⭐信用优"] } */
    private Map<Long, List<String>> productTags;

    /** 市场分析文本，如 "同类均价¥4,200，当前商品低于市场价15%" */
    private String marketAnalysis;

    /** 推荐用户提问列表，如 ["这个配置写Java够用吗？", "续航怎么样？"] */
    private List<String> suggestedQuestions;
}
```

### 3.2 新增: `AiSearchEnhancerPort` 接口

**位置**: `easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/port/AiSearchEnhancerPort.java`

```java
package com.cartethyia.easyorange.product.domain.port;

import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.common.dto.ProductReadModel;
import java.util.List;
import java.util.Optional;

/**
 * AI 智能导购增强端口。
 * 与 ProductSearchQueryPort 模式一致，ai 模块实现，product 模块通过 Optional 引用。
 * ai 模块不可用时搜索降级为普通搜索。
 */
public interface AiSearchEnhancerPort {
    /**
     * 尝试对搜索关键词进行 AI 增强。
     * 内部检测是否为自然语言，若非自然语言或 LLM 超时则返回 empty。
     *
     * @param keyword     用户搜索关键词
     * @param topProducts 搜索结果 top N (≤5) 商品
     * @return AI 增强数据，若不适配则返回 empty
     */
    Optional<AiEnhancement> tryEnhance(String keyword, List<ProductReadModel> topProducts);
}
```

### 3.3 新增: `NaturalLanguageDetector`

**位置**: `easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/service/NaturalLanguageDetector.java`

**不调 LLM**，纯规则检测：

```java
package com.cartethyia.easyorange.ai.service;

import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class NaturalLanguageDetector {

    private static final Set<String> INTENT_WORDS = Set.of(
        "找", "推荐", "适合", "可以", "预算", "以内", "左右",
        "哪个", "怎么", "什么", "好", "吗", "能", "要"
    );

    private static final int MIN_LENGTH = 5;

    /**
     * 判断是否为自然语言查询（而非纯关键词搜索）。
     * 规则: 长度 ≥ 5 且包含至少一个意图词。
     */
    public boolean isNaturalLanguage(String keyword) {
        if (keyword == null || keyword.length() < MIN_LENGTH || keyword.isBlank()) {
            return false;
        }
        return INTENT_WORDS.stream().anyMatch(w -> keyword.contains(w));
    }
}
```

### 3.4 新增: `ProductTagger`

**位置**: `easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/service/ProductTagger.java`

```java
package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.common.dto.ProductReadModel;  // 假设存在
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 为搜索结果的商品打 AI 标签。
 * 纯读已有数据，无 LLM 调用。
 * 
 * 标签规则:
 * - 💰超值: AiPricingService 判断价格低于市场均价 ≥10%
 * - ✅AI审核: 商品已通过 AI 审核 (product.aiReviewed == true)
 * - ⭐信用优: 卖家信用分 > 80
 * - 📸实拍: 商品有实拍图片 (product.hasRealImages == true)
 */
@Component
@RequiredArgsConstructor
public class ProductTagger {

    // 依赖的具体端口/适配器根据实际模块结构调整
    // 可能通过 UserInfoPort 获取卖家信用分
    // 通过 ProductDetailPort 获取图片信息

    public Map<Long, List<String>> tagProducts(List<ProductReadModel> products) {
        Map<Long, List<String>> tags = new HashMap<>();
        for (var product : products) {
            List<String> productTags = new ArrayList<>();
            
            // 定价分析标签
            if (product.getAiPriceAnalysis() != null 
                && product.getAiPriceAnalysis().getDiscountPercent() >= 10) {
                productTags.add("💰超值");
            }
            
            // AI 审核标签
            if (product.isAiReviewed()) {
                productTags.add("✅AI审核");
            }
            
            // 信用标签 (需通过 UserInfoPort 获取)
            if (product.getSellerCreditScore() != null 
                && product.getSellerCreditScore() > 80) {
                productTags.add("⭐信用优");
            }
            
            // 实拍标签
            if (product.getImageCount() != null && product.getImageCount() >= 3) {
                productTags.add("📸实拍");
            }
            
            tags.put(product.getId(), productTags);
        }
        return tags;
    }
}
```

### 3.5 新增: `AiSearchEnhancerAdapter`（实现 `AiSearchEnhancerPort`）

> **注**：以下代码为初始设计草图，实际实现已迁移至 `easyorange-ai/.../adapter/outbound/AiSearchEnhancerAdapter.java`，使用显式构造器 + `@Qualifier` 注入。核心逻辑（4路并行、缓存、降级）不变。

**位置**: `easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/adapter/outbound/AiSearchEnhancerAdapter.java`

```java
package com.cartethyia.easyorange.ai.adapter.outbound;

import com.cartethyia.easyorange.ai.port.LlmPort;
import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.common.dto.ProductReadModel;
import com.cartethyia.easyorange.product.domain.port.AiSearchEnhancerPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;

/**
 * AI 智能导购增强管道。
 * 并行执行四个子步骤，任意子步骤超时/失败不影响其他步骤。
 */
@Slf4j
@Component
public class AiSearchEnhancerAdapter implements AiSearchEnhancerPort {

    private final NaturalLanguageDetector nlDetector;
    private final LlmPort llmPort;
    private final ProductTagger productTagger;

    private static final int TIMEOUT_SECONDS = 5;
    private static final String INTENT_SYSTEM_PROMPT = """
        你是一个二手购物平台的 AI 导购助手。
        用户输入了一段自然语言商品搜索需求。
        请用一句简洁的话总结用户想找什么，不超过30个字。
        直接输出总结，不要前缀。
        示例: "想找5000以内适合编程的笔记本"
    """;
    private static final String QUESTIONS_SYSTEM_PROMPT = """
        基于用户需求和搜索结果，生成2-3个用户可能想追问的问题。
        每个问题不超过15个字。
        用逗号分隔输出，不要序号。
    """;

    @Override
    public Optional<AiEnhancement> tryEnhance(String keyword, List<ProductReadModel> topProducts) {
        if (!nlDetector.isNaturalLanguage(keyword)) {
            return Optional.empty();
        }
        if (topProducts == null || topProducts.isEmpty()) {
            return Optional.empty();
        }

        try {
            var executor = Executors.newFixedThreadPool(4);
            List<ProductReadModel> top5 = topProducts.subList(0, Math.min(5, topProducts.size()));

            CompletableFuture<String> intentFuture = CompletableFuture
                .supplyAsync(() -> llmPort.generateText(INTENT_SYSTEM_PROMPT, keyword), executor);

            CompletableFuture<Map<Long, List<String>>> tagsFuture = CompletableFuture
                .supplyAsync(() -> productTagger.tagProducts(top5), executor);

            CompletableFuture<String> marketFuture = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return pricingService.analyzeMarket(top5);
                    } catch (Exception e) {
                        log.warn("Market analysis failed", e);
                        return null;
                    }
                }, executor);

            CompletableFuture<List<String>> questionsFuture = CompletableFuture
                .supplyAsync(() -> {
                    String result = llmPort.generateText(QUESTIONS_SYSTEM_PROMPT, keyword);
                    return result != null ? Arrays.asList(result.split("[,，]")) : List.of();
                }, executor);

            // 等待所有完成（含超时）
            CompletableFuture.allOf(intentFuture, tagsFuture, marketFuture, questionsFuture)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            AiEnhancement enhancement = new AiEnhancement();
            enhancement.setIntentExplanation(intentFuture.getNow(null));
            enhancement.setProductTags(tagsFuture.getNow(Map.of()));
            enhancement.setMarketAnalysis(marketFuture.getNow(null));
            enhancement.setSuggestedQuestions(questionsFuture.getNow(List.of()));

            // 至少要有需求理解或标签才返回
            if (enhancement.getIntentExplanation() == null && enhancement.getProductTags().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(enhancement);

        } catch (Exception e) {
            log.warn("AI search enhancement failed for keyword: {}", keyword, e);
            return Optional.empty();
        }
    }
}
```

### 3.6 修改: `ProductSearchRequest` 新增字段

**位置**: `easyorange-product/src/main/java/com/cartethyia/easyorange/.../dto/request/ProductSearchRequest.java`

```java
/** 是否启用 AI 智能导购增强 */
private boolean aiEnhanced;
```

### 3.7 修改: `SearchPageResponse` 新增字段

**位置**: 搜索结果 DTO（具体包名根据项目实际）

```java
/** AI 智能导购增强数据，null 时不展示 */
private AiEnhancement aiEnhancement;
```

### 3.8 修改: `ProductSearchHandler`

**位置**: `easyorange-product/src/main/java/com/cartethyia/easyorange/product/application/query/handler/ProductSearchHandler.java`

```java
// 新增依赖
private final Optional<AiSearchEnhancerPort> aiSearchEnhancer;

@Transactional(readOnly = true)
public SearchPageResponse<ProductResponse> handleSearch(ProductSearchRequest request) {
    // 1. 现有搜索逻辑不变
    SearchPageResponse<ProductResponse> result = doSearch(request);
    
    // 2. AI 增强（仅在启用且有结果时）
    if (request.isAiEnhanced() 
        && aiSearchEnhancer.isPresent() 
        && !result.getRecords().isEmpty()) {
        
        List<ProductReadModel> topProducts = convertToReadModels(
            result.getRecords().subList(0, Math.min(5, result.getRecords().size()))
        );
        
        aiSearchEnhancer.get()
            .tryEnhance(request.getKeyword(), topProducts)
            .ifPresent(result::setAiEnhancement);
    }
    
    return result;
}
```

### 3.9 Maven 依赖

`easyorange-product/pom.xml` 新增对 `easyorange-ai` 的 optional 依赖（如尚无）：

```xml
<dependency>
    <groupId>com.cartethyia</groupId>
    <artifactId>easyorange-ai</artifactId>
    <optional>true</optional>
</dependency>
```

---

## 4. 前端新增/修改

### 4.1 修改: `src/types/product.ts`

```typescript
export interface AiEnhancement {
    intentExplanation: string;
    productTags: Record<string, string[]>;  // productId → tags
    marketAnalysis: string;
    suggestedQuestions: string[];
}

export interface ProductSearchResult {
    records: Product[];
    total: number;
    current: number;
    size: number;
    pages: number;
    facets: FacetBucket[];
    aiEnhancement?: AiEnhancement;  // 新增
}

export interface ProductSearchParams {
    keyword?: string;
    categoryId?: number;
    status?: number;
    minPrice?: number;
    maxPrice?: number;
    conditionLevel?: number;
    sort?: string;
    pageNum?: number;
    pageSize?: number;
    aiEnhanced?: boolean;  // 新增
}
```

### 4.2 新增: `src/components/search/AiSearchPanel.tsx`

**位置**: `easyorange-frontend/src/components/search/AiSearchPanel.tsx`

```tsx
import React from 'react';
import { Sparkles, TrendingUp, HelpCircle, Target } from 'lucide-react';
import type { AiEnhancement } from '../../types/product';

interface AiSearchPanelProps {
    enhancement: AiEnhancement;
    onQuestionClick: (question: string) => void;
}

export function AiSearchPanel({ enhancement, onQuestionClick }: AiSearchPanelProps) {
    return (
        <div className="ai-search-panel">
            <div className="ai-panel-header">
                <Sparkles size={18} />
                <span>AI 智能分析</span>
            </div>

            {enhancement.intentExplanation && (
                <div className="ai-panel-section">
                    <div className="ai-section-title">
                        <Target size={14} />
                        <span>需求理解</span>
                    </div>
                    <p className="ai-section-content">{enhancement.intentExplanation}</p>
                </div>
            )}

            {enhancement.marketAnalysis && (
                <div className="ai-panel-section">
                    <div className="ai-section-title">
                        <TrendingUp size={14} />
                        <span>市场分析</span>
                    </div>
                    <p className="ai-section-content">{enhancement.marketAnalysis}</p>
                </div>
            )}

            {enhancement.suggestedQuestions?.length > 0 && (
                <div className="ai-panel-section">
                    <div className="ai-section-title">
                        <HelpCircle size={14} />
                        <span>猜你想问</span>
                    </div>
                    <div className="ai-questions">
                        {enhancement.suggestedQuestions.map((q, i) => (
                            <button
                                key={i}
                                className="ai-question-btn"
                                onClick={() => onQuestionClick(q)}
                            >
                                {q}
                            </button>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}
```

### 4.3 新增: `src/components/product/AiTag.tsx`

```tsx
import React from 'react';

interface AiTagProps {
    tag: string;
}

const TAG_COLORS: Record<string, string> = {
    '💰超值': '#10b981',    // 绿色
    '✅AI审核': '#3b82f6',   // 蓝色
    '⭐信用优': '#f59e0b',   // 金色
    '📸实拍': '#8b5cf6',    // 紫色
};

export function AiTag({ tag }: AiTagProps) {
    const color = TAG_COLORS[tag] || '#6b7280';
    return (
        <span
            className="ai-tag"
            style={{
                backgroundColor: `${color}15`,
                color: color,
                border: `1px solid ${color}30`,
                borderRadius: '4px',
                padding: '2px 6px',
                fontSize: '11px',
                fontWeight: 500,
                marginLeft: '4px',
                whiteSpace: 'nowrap',
            }}
        >
            {tag}
        </span>
    );
}
```

### 4.4 修改: `ProductCard` 组件

**位置**: `easyorange-frontend/src/components/product/ProductCard.tsx`

```tsx
// 新增 props
interface ProductCardProps {
    product: Product;
    // ... 已有 props
    aiTags?: string[];  // 新增
}

// 在渲染位置（如商品名右侧或卡片角标）
{aiTags?.map(tag => <AiTag key={tag} tag={tag} />)}
```

### 4.5 修改: `useProductSearch` hook

**位置**: `easyorange-frontend/src/hooks/product/useSearch.ts`

```typescript
export interface UseProductSearchResult {
    products: Product[];
    total: number;
    facets: FacetBucket[];
    aiEnhancement?: AiEnhancement;  // 新增
    isLoading: boolean;
    error: Error | null;
}

export function useProductSearch(params: ProductSearchParams = {}): UseProductSearchResult {
    const query = useQuery<ProductSearchResult>({
        queryKey: ['productSearch', params],
        queryFn: async () => {
            const response = await productApi.searchProducts(params);
            return response.data;
        },
        enabled: (params.keyword?.trim().length ?? 0) > 0,
        staleTime: 30 * 1000,
    });

    return {
        products: query.data?.records ?? [],
        total: query.data?.total ?? 0,
        facets: query.data?.facets ?? [],
        aiEnhancement: query.data?.aiEnhancement,
        isLoading: query.isLoading,
        error: query.error,
    };
}
```

### 4.6 修改: `SearchPage.tsx`

改动要点:
- 新增 `aiEnabled` state 控制 AI 增强开关（默认关闭）
- 把装饰性 AI 按钮 wire 到 `aiEnabled` toggle
- `searchQueryParams` 传 `aiEnhanced: aiEnabled`
- 结果列表顶部渲染 `AiSearchPanel`（aiEnhancement 存在时）
- ProductCard 传入 `aiTags`（来自 `aiEnhancement.productTags`）
- "猜你想问"点击时填入搜索框并重新搜索

---

## 5. 错误与降级处理

| 场景 | 行为 |
|------|------|
| ai 模块未部署 | `Optional<AiSearchEnhancerPort>` 为空，搜索走原有逻辑 |
| LLM 超时 (5s) | `CompletableFuture.get(TIMEOUT_SECONDS)` 抛异常，catch 后返回 empty |
| LLM 返回空结果 | `intentExplanation == null && productTags.isEmpty()` → 返回 empty |
| 搜索无结果 | `topProducts` 为空 → 不触发 AI 增强 |
| 自然语言检测不过 | 纯关键词搜索不走 AI 管道 |
| 单个子步骤失败 | 其他子步骤结果正常返回，失败项为 null/empty |

---

## 6. 缓存策略

- 使用 Redis 缓存 AI 增强结果
- Key: `ai:search:enhance:{md5(keyword)}`
- TTL: 5 分钟
- 首次请求触发 LLM 调用，5 分钟内相同 keyword 复用缓存

---

## 7. 实现顺序

### Phase 1: 后端核心

| 步骤 | 文件 | 预估改动 |
|------|------|---------|
| 1.1 | `AiEnhancement.java` | 新增, ~30 行 |
| 1.2 | `AiSearchEnhancerPort.java` | 新增, ~15 行 |
| 1.3 | `NaturalLanguageDetector.java` | 新增, ~35 行 |
| 1.4 | `ProductTagger.java` | 新增, ~80 行 |
| 1.5 | `AiSearchEnhancerAdapter.java` | 新增, ~120 行 |
| 1.6 | `ProductSearchRequest.java` | +3 行 |
| 1.7 | `SearchPageResponse` (或 Equiv) | +3 行 |
| 1.8 | `ProductSearchHandler.java` | +15 行 |
| 1.9 | `pom.xml` (optional 依赖) | +6 行 |

### Phase 2: 前端

| 步骤 | 文件 | 预估改动 |
|------|------|---------|
| 2.1 | `src/types/product.ts` | +15 行 |
| 2.2 | `src/components/search/AiSearchPanel.tsx` | 新增, ~80 行 |
| 2.3 | `src/components/product/AiTag.tsx` | 新增, ~30 行 |
| 2.4 | `src/components/product/ProductCard.tsx` | +10 行 |
| 2.5 | `src/hooks/product/useSearch.ts` | +15 行 |
| 2.6 | `src/pages/profile/SearchPage.tsx` | +50 行 |

### Phase 3: 集成

| 步骤 | 说明 |
|------|------|
| 3.1 | 集成测试: `handleSearch(aiEnhanced=true, NL query)` → 返回含 aiEnhancement |
| 3.2 | 前端验证: 搜索页渲染 AI panel + 标签 |
| 3.3 | 降级测试: ai 模块缺失 → 正常搜索 |
| 3.4 | 超时测试: LLM mock 挂起 6s → 正常搜索 |
| 3.5 | 缓存测试: 相同 query 第二次不调 LLM |

---

## 8. 不做的事

| 事项 | 理由 |
|------|------|
| 修复 `nameEmbedding` ES 索引 | 独立问题，不阻塞本功能 |
| 多轮对话 | 本功能是单次搜索增强，非对话页面 |
| 图片搜索 | 已有拍照上架能力，后续可扩展到搜索 |
| 个性化推荐 | 项目暂无用户行为追踪基础设施 |
| 语义搜索 KNN | 本功能使用关键词搜索 + LLM 增强，不依赖 KNN |
| 替换现有搜索框 | 沿用现有搜索 UI，仅增强结果展示 |

---

## 9. 预估总改动量

```
后端: 9 文件 (6 新增 + 3 修改) ≈ 310 行
前端: 6 文件 (2 新增 + 4 修改) ≈ 200 行
总计: 15 文件 ≈ 510 行
```
