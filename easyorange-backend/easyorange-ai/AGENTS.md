# easyorange-ai 模块指南

AI 能力模块，Port/Adapter 六边形架构。所有 LLM 调用通过端口接口隔离，业务逻辑不依赖具体 AI 供应商。

## 目录结构

```
ai/
├── port/                           # 端口接口 (domain 定义)
│   ├── LlmPort.java                # 大语言模型端口，generateText(prompt, context)
│   └── VisionPort.java             # 视觉识别端口，analyzeImage(image)
├── adapter/                        # 适配器 (AI 供应商实现)
│   ├── DeepSeekLlmAdapter.java     # DeepSeek 文本模型实现
│   ├── QwenVlVisionAdapter.java    # 通义千问 VL 视觉模型实现
│   ├── CachingLlmAdapter.java      # @Primary 装饰器，L1+L2 缓存
│   ├── CachingVisionAdapter.java   # @Primary 装饰器，L1+L2 缓存
│   ├── outbound/
│   │   └── AiSearchEnhancerAdapter.java  # AI 导购搜索增强管道 (4 路并行)
│   └── dto/                        # 适配器 DTO
├── interceptor/
│   └── AiRateLimitInterceptor.java # AI 限流拦截器，Redis 令牌桶 + stale 降级
├── enums/
│   └── AiCallScope.java            # 6 个枚举：PRICING/REVIEW/QA/COPY/AUTO_LISTING/SEMANTIC
├── service/                        # 业务服务
│   ├── NaturalLanguageDetector.java   # 规则引擎 (intent words + 长度检测)
│   ├── ProductTagger.java             # 商品标签引擎 (折扣/图片/信用分)
│   ├── CreditScoreFetcher.java        # 信用分获取接口
│   ├── JdbcCreditScoreFetcher.java    # 信用分获取实现 (批量 JDBC + 降级)
│   ├── AiPricingService.java          # 智能定价
│   ├── AiReviewService.java           # AI 审核
│   ├── AiQaService.java               # 智能问答
│   ├── AiCopyGenerationService.java   # 智能文案
│   ├── AutoListingService.java        # 拍照上架
│   ├── SemanticSearchService.java     # 语义搜索
│   └── CreditScoringService.java      # 信用评分
├── dto/                            # 业务 DTO
├── config/
│   ├── AiProperties.java           # AI 配置属性 (API key, endpoint, 模型名, cache, rateLimit)
│   ├── AiConfig.java               # AI Bean 配置
│   └── AiCacheConfig.java          # 6 个 MultiLevelCache Bean + 拦截器注册
└── controller/                     # API 接口 (可选, 部分控制器在 easyorange-application)
```

## 架构决策

- **Port/Adapter 隔离**: `port/` 定义接口，`adapter/` 实现具体供应商。新增 AI 供应商只需新增 adapter 类，不修改业务代码
- **跨模块 Port**: 本模块作为端口实现方，接口定义在 consumer 模块（如 `AiSearchEnhancerPort` 在 `easyorange-product` 的 `domain/port/`），通过 Spring `Optional<>` 注入实现运行时可替换
- **纯规则零 LLM**: `NaturalLanguageDetector` 和 `ProductTagger` 不调任何 LLM，通过规则引擎 + 数据库查询完成，确保亚毫秒级响应
- **并行容错**: `AiSearchEnhancer` 内 4 个子步骤使用 `CompletableFuture` 并行执行，单步骤超时/失败不影响其他步骤。5s 总超时控制
- **缓存装饰器**: `CachingLlmAdapter` / `CachingVisionAdapter` 使用 `@Primary` 装饰模式，L1 (Caffeine 5min) + L2 (Redis tiered TTL)，业务服务零修改
- **限流拦截器**: `AiRateLimitInterceptor` 拦截 `/api/ai/**`，按端点独立令牌桶 (5-30次/分)，超限时优先返回 stale 缓存

## 缓存与限流

| 端点 | L2 TTL | 限流 (次/分) |
|------|--------|-------------|
| pricing | 1h | 10 |
| review | 1h | 10 |
| generate-copy | 1h | 20 |
| auto-listing | 1h | 5 |
| semantic-search | 1h | 30 |
| qa | 15min | 20 |

**配置**：`application.yaml` → `easyorange.ai.cache.*` + `easyorange.ai.rate-limit.*`

## 新增 AI 能力

1. 在 `port/` 定义接口（如果 consumer 也在本模块）或在 consumer 模块 `domain/port/` 定义接口
2. 在 `service/` 实现业务逻辑
3. 如果涉及 LLM 调用，通过 `LlmPort` 或 `VisionPort` 进行
4. 在 `AiSearchEnhancerAdapter` 中添加新步骤（如果是搜索增强管道的一部分）
5. 在 `AiCallScope` 枚举中新增条目，配置 TTL 和限流阈值

## 搜索增强管道 (AiSearchEnhancer)

```
用户输入自然语言查询 (如 "5000以内适合编程的笔记本")
    ↓
NaturalLanguageDetector.isNaturalLanguage()  → false → 降级为普通搜索
    ↓ (true 且 aiEnhanced=true)
AiSearchEnhancer
    ├─ Future 1: LLM → 需求理解 (intentExplanation)
    ├─ Future 2: ProductTagger → 商品标签 (productTags)
    ├─ Future 3: LLM → 市场分析 (marketAnalysis)
    └─ Future 4: LLM → 猜你想问 (suggestedQuestions)
    ↓
RedisCache (5min TTL, 可选：无 Redis 时不缓存)
    ↓
AiEnhancement DTO → SearchPageResponse.aiEnhancement
```

## 单元测试

| 测试类 | 行数 | 覆盖场景 |
|--------|------|---------|
| `NaturalLanguageDetectorTest` | ~22 | null/空白/长度边界/意图词组合 |
| `ProductTaggerTest` | ~12 | 折扣/图片/信用分/综合场景 |
| `AiSearchEnhancerTest` | ~9 | 前置条件/缓存命中/正常流程/异常降级 |
| `AiCallScopeTest` | ~12 | URI 映射/TTL/限流配置 |
| `CachingLlmAdapterTest` | ~4 | 缓存禁用/embedding 不缓存 |
| `CachingVisionAdapterTest` | ~3 | 缓存禁用/单图/多图 |
| `AiRateLimitInterceptorTest` | ~5 | 非 AI 路径/限流/fail-open/429 |