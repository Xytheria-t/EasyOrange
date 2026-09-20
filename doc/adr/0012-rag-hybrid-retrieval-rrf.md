# ADR 0012 — RAG 检索改「两路独立召回 + RRF 排名融合」，去掉无效的 Java 侧余弦重排

- **状态**：接受
- **日期**：2026-09-17
- **决策者**：后端架构
- **标签**：`ai` `rag` `retrieval` `elasticsearch` `evaluation`

---

## 上下文（Context）

知识库检索（RAG）的第一版实现是「ES 同请求合并 kNN + BM25 → Java 侧按余弦相似度重排 → 取 topK」。
2026-09 复盘发现三个事实：

1. **余弦重排对稠密召回是恒等变换**：重排用的 `cosine(queryEmbedding, chunkEmbedding)` 与 kNN 那一路用的是同一个 embedding 模型的同一个度量，对稠密召回结果是单调变换 —— 排序不变。它唯一实际影响的是「BM25 召回来、但稠密没召回」的那些候选，代价却是把 BM25 的**排序信号**（谁更相关）整体丢弃，只剩「进没进候选池」这一个 0/1 信息。
2. **ES 同请求拿不到两路排名**：`knn` 与 `query` 同时下发时，ES 按内部规则把两路分数合成一个分值，客户端只拿到合并后的顺序 —— 想按排名融合就必须分两次查询。
3. **检索评测指标当前没有判别力**：种子语料只有 5 篇文档（kb-0001~0005，每篇 1 块），而 `RETRIEVAL_TOP_K = 5`、kNN 的 `k = topK * 2 = 10` —— 每次检索必然把全库取回，hit@5 恒为 100%，与检索质量无关。CI 的 `hit@5 ≥ 0.5` 断言实际测的是「ES 开没开」（ES 关闭走 MySQL LIKE 时实测 10%）。

约束：

- **约束 1 — 不引入新组件**：SKU 与语料量级远未到独立向量库（Milvus 等）的门槛，检索仍必须在 ES 内完成，不增加运维面。
- **约束 2 — 依赖方向不变**：检索融合是索引侧职责，业务侧（`easyorange-ai`）不应拿到 ES 客户端或分块向量。
- **约束 3 — 评测必须能回归**：任何排序策略改动都要能被金标准集量化（hit@5 / MRR），否则无法判断改好还是改坏。

## 决策（Decision）

**检索改为两路独立召回（kNN / BM25 各查一次）后在索引侧做 RRF 排名融合；删除 Java 侧余弦重排，命中不再回传分块向量；同时把语料扩到 23 篇（大部分是同域干扰文档）让 hit@5 恢复判别力。**

1. **融合算法**：RRF（Reciprocal Rank Fusion，Cormack et al. 2009），`score(d) = Σ 1/(k + rank_r(d))`，`k = 60`。只用排名不用分数 —— 余弦相似度与 BM25 分值量纲不可比，直接加权求和需要调参且对分布漂移敏感。实现为纯函数 `RrfFusion.fuse(k, rankedIdLists)`，稳定排序保证评测可复现。
2. **单路失败降级为单路召回**：任一腿查询异常只告警并继续用另一腿，不整体空手而归。
3. **检索端口契约改为返回 `KnowledgeMatch`**（docId/chunkIndex/title/content/score），**不再返回 `KnowledgeChunk`**（带 1024 维向量）：排序已在索引侧定稿，业务侧不需要向量；ES 查询用 `_source` 排除 `embedding`（每条约 10KB 的 JSON 不再回传）。
4. **语料扩容**：新增 kb-0006~kb-0025 共 20 篇**同域干扰文档**（也是真实平台规则，非随机噪声），使 `topK=5` 面对全库分块成为真正的选择问题；金标准集相应拆分为 20 条 `scope: chat` + 15 条 `scope: retrieval`（新增 5 条覆盖干扰文档），并由 `GoldenSetLoader` 在加载期强校验 scope 与字段自洽性。后续 kb-0004 / kb-0008 随信用、举报功能下线删除，语料定稿 23 篇（ID 不复用）。
5. **门禁补覆盖率维度**：`EvalGate` 除分数外新增 `checkCoverage`，评审覆盖率低于 80% 判失败（均分只统计幸存用例，1 条打 5 分也能蒙过分数门禁）。

关键实现：

- 融合：[RrfFusion.java](../../easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/domain/model/RrfFusion.java)
- 索引侧两路召回：[KnowledgeElasticsearchAdapter.java](../../easyorange-backend/easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/KnowledgeElasticsearchAdapter.java)
- 检索服务：[KnowledgeRetrievalService.java](../../easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/application/service/KnowledgeRetrievalService.java)
- 语料与评测集：[R__seed_knowledge_docs.sql](../../easyorange-backend/easyorange-application/src/main/resources/db/migration/R__seed_knowledge_docs.sql)、[golden-set.yaml](../../easyorange-backend/easyorange-ai/src/main/resources/eval/golden-set.yaml)

## 后果（Consequences）

### 正向后果

- BM25 的排序信号真正进入最终排序，中长尾查询（词面对得上但语义向量漂移）不再被稠密路完全压制。
- 排序逻辑从「看起来有重排」变成可解释的两段式：每路各自排名 → 排名融合。
- 检索响应体显著变小：命中不再携带 1024 维向量，同时省掉每条候选的 JSON 浮点解析。
- hit@5/MRR 恢复判别力：语料规模远大于 topK，指标重新能反映「选没选对」；干扰文档同时扩了评测集的覆盖面。
- 融合逻辑是可以单测的纯函数（`RrfFusionTest`），不依赖 ES。

### 负向后果

- **ES 往返从 1 次变 2 次**：小索引上是毫秒级，但 QPS 高时是实打实的开销。
- **`k = 60` 与候选池倍数（`topK * 2`）是经验值**：没有在真实流量上做过参数扫描，可能不是本项目语料下的最优。
- **旧口径指标作废**：`doc/工程指标.md` 里 2026-09-17 的 `hit@5 100% / MRR 0.95` 是 5 篇语料下测的，扩语料后必须重跑才能引用；本次改动未在本地实测（本地无 AI key），由 `ai-eval.yml` 回归给出首个可信数字。
- **单条命中内容可能变长**：融合后进入 top5 的文档分布更分散（不再被稠密路垄断），prompt 里拼接的片段来源更杂，需要靠引用溯源观察。

### 缓解措施

- 单路失败降级为单路召回，避免次要矛盾（BM25 抖动）升级为主要矛盾（零召回）。
- `GoldenSetCorpusTest`（常驻、不需要 API key）断言「gold_doc_ids 必须存在于种子语料」「语料规模 ≥ topK × 3 倍」「种子文档均为单块」，防止语料被删回与 topK 同量级而指标悄悄失去意义。
- 检索指标逐条落 `eo_retrieval_metric`（含 run_id 与每条 hit 命中位置），参数调整可基于历史数据回看。

## 备选方案（Alternatives Considered）

- **ES 原生 RRF retriever（`retrievers: [{rrf: ...}]`）**：ES 已内置，但属于 retriever DSL，Spring Data Elasticsearch 没有对应 API，只能绕开 `ElasticsearchOperations` 直接发原始请求 —— 为省一个纯函数而破坏全模块统一的查询出口，不划算。若未来升级 SDES 并支持该 DSL，可平移替换本实现。
- **保留余弦重排，另加 cross-encoder 重排模型**：效果上限更高，但要引入新的推理调用（成本、延迟、又一个供应商），且在当前语料规模下无法验证收益（指标刚恢复判别力，加变量会让归因变模糊）。触发条件：语料上到千级分块且 hit@5 触顶（≥95%）而 MRR 偏低时再评估。
- **加权分数融合（weighted score fusion）**：需要把余弦相似度与 BM25 分值归一化到可比区间，归一化参数随语料分布漂移，是典型的「调参负债」；RRF 用排名天然免调参。
- **直接加大 topK 或换更大 embedding 模型**：回避问题本身。增加 topK 会把更多噪声塞进 prompt，换模型不解决「两路信号怎么合」的结构问题。

## 备注（Notes）

- 相关 ADR：Related to [ADR-0008](0008-ai-spring-ai-framework.md)（Spring AI 框架化）、[ADR-0004](0004-ai-bulkhead-token-budget.md)（Token 预算仍现役）
- 相关文档：[easyorange-backend/AGENTS.md](../../easyorange-backend/AGENTS.md)「模块要点 → ai」（RAG 检索）、[doc/工程指标.md](../工程指标.md)（AI 能力表）
- 相关代码：`RrfFusion` / `KnowledgeElasticsearchAdapter` / `KnowledgeMatch` / `KnowledgeRetrievalService`
- 后续演进触发条件：语料分块数超过 ES `num_candidates`（100）的量级，或 hit@5 触顶而 MRR 停滞时，重新评估重排模型与独立向量库
- 决策回顾周期：下次 `ai-eval.yml` 回归给出新口径数字后回看（按需 `workflow_dispatch` 触发，单次约 60 次真实模型调用）
