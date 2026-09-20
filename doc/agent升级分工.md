# Agent 升级分工（双会话并行）

> **临时协调文档**：实施完毕即删——结论落进 [工程指标.md](工程指标.md) 与代码注释，工单本身不留（同已删除的数字治理工单）。
> 诊断与理由不在本文重复，见 [agent诊断与升级路线.md](agent诊断与升级路线.md)。
> 本文只冻结三件事：**谁改哪些文件**、**接口签名**、**验收命令**。

## 一、拆分轴：文件所有权，不是难度

难点不在推理量，在上下文：接线端要同时握住 DDD 分层、ArchUnit 门禁、prompt 模板约定、指标 key 约定、循环控制权归属、前端 SSE 协议；纯计算端只需要领域语义。把接线端交给已持有上下文的会话，纯计算端交给新会话，两边都不用重建上下文——所以按**文件所有权**切，不按难度切。

| lane | 交付 | 文件权限 |
|---|---|---|
| **A 纯计算** | `AssetComparison` + `PriceStats`，各带单测 | **只新建文件**，不得修改任何已存在文件 |
| **B 接线** | 工具注册 / 循环 / prompt / 配置 / 文档校准 | 既有文件只由 B 改，A 一律不碰 |

**两条硬规则**

1. **同一时刻只允许一方跑 `./mvnw`。** 并发跑会撞 `target/` 与本地仓库——[常用命令.md:23](doc/agents/常用命令.md) 记录的「单模块 `-pl` 前须 `install` 刷新，否则按旧已安装 jar 编译、改过的接口签名假失败」就是这类冲突。跑前在会话里说一声。
2. **双方都禁 `git add -A`**（仓库既有规则）。A 的提交只含自己新建的两个类与两个测试。

## 二、目标形态：工具面从「全是检索」变成「检索 + 计算」

改造前 4 个工具全是只读检索，循环存在的唯一意义是换关键词重试——这是「项目不像 Agent」的根因。改造后：

| 工具 | 类别 | 归属 | 说明 |
|---|---|---|---|
| `knowledge_search` | 检索 | 既有 | 平台规则 |
| `product_search` | 检索 | 既有 | 找货 |
| `product_detail` | 检索 | 既有 | 单件详情 |
| `market_price_stats` | **计算** | A2 | 对已召回资产算行情（件数 / 均价 / 区间），模型据此判断「值不值」 |
| `compare_assets` | **计算** | A1 | 对 2-4 件候选逐维确定性比对，一次替代最多 4 次 `product_detail` |
| `remember_preference` | **写入** | B | 长期记忆写入，从 `finish` 的参数副作用提升为模型自主决策的一步 |
| `finish` | 收敛 | 既有 | 瘦身：不再携带 preference 参数 |

**为什么要加计算类工具**：`compare_assets` 让第 N 步必须依赖第 N-1 步的输出——「比哪几件」是模型对搜索结果的决策，循环因此有了存在理由。它同时把最多 4 次 `product_detail` 压成 1 步，所以步数上限从 5 放宽到 7 不是净增成本。

**`finish` 拆分另有一个功能理由**（不只是叙事）：现在 5 步跑满（`step_limit`）、预算耗尽或决策失败时 `finish` 不执行，偏好**静默丢失**；独立成工具后模型一发现偏好就能落库，不再依赖收敛成功。

**命名说明**：agent 工具面用 `market_price_stats`，与搜索管道里既有的 `market_analysis`（`SearchTool` 面）刻意不同名——两个注册表互不相干（内部工具 / MCP 工具已各自命名），但同名会让 review 时 grep 出两个落点。

## 三、A 的交付物（接口冻结，签名不得改）

签名的调用方（`AgentTools`）由 B 编写，改签名会让 B 返工，所以这里冻结。参照同目录 `RrfFusion` 的形态：final 类 + 静态工厂 + 内嵌 record + 中文注释说约束。

### A1 `AssetComparison`

落点 `easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/domain/model/AssetComparison.java`
测试 `easyorange-ai/src/test/java/com/cartethyia/easyorange/ai/domain/model/AssetComparisonTest.java`

```java
public final class AssetComparison {

    /** 少于 2 件无法比较。 */
    public static final int MIN_CANDIDATES = 2;

    /** 多于 4 件时观察文本会淹没下一轮 prompt，调用方负责截断。 */
    public static final int MAX_CANDIDATES = 4;

    /** 不足 MIN_CANDIDATES 件时返回 empty，由调用方转成模型可读的失败观察。 */
    public static Optional<AssetComparison> of(List<AssetDetail> details);

    /** 逐维差异（价格 / 成色 / 地区 / 在售状态），每维标出胜出方；无差异的维度不进结果。 */
    public List<Dimension> dimensions();

    /** 进下一轮 prompt 的观察文本：紧凑纯文本，不带 JSON 引号（同 AgentTools 的观察约定）。 */
    public String observation();

    /** 单维结论：维度名、胜出资产 ID（无胜出方时为 null）、一句话依据。 */
    public record Dimension(String name, String winnerProductId, String note) {}
}
```

要点：
- **维度选择、胜出判定、note 文案全部是 A 的领域判断**，这是本任务真正难的部分：`price` 为 null（面议）不能当最低价胜出；`conditionDesc` 是自由文本，判定规则要写清（无法判定时该维不进结果，不要硬编）；`status` 不是「在售」时要显式点出来，因为它直接决定还推不推荐得出口。
- **不用 `@Nullable`**：`AssetDetail` 的 `price` / `conditionDesc` / `location` / `status` 全部可空，用显式判空处理（理由见 §四.1）。

### A2 `PriceStats`

落点 `easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/domain/model/PriceStats.java`
测试 `easyorange-ai/src/test/java/com/cartethyia/easyorange/ai/domain/model/PriceStatsTest.java`

```java
public final class PriceStats {

    /** 无有效价格（空列表 / 全为 null / 全为非正）时返回 empty。 */
    public static Optional<PriceStats> of(List<AssetHit> hits);

    public int count();

    public BigDecimal min();

    public BigDecimal max();

    /** 均价，HALF_UP 取整到整数——与 MarketAnalysisTool 口径一致，不要改精度。 */
    public BigDecimal avg();

    /** 「当前 N 件在售，均价 ¥X，价格区间 ¥A-¥B」；min 等于 max 时说「均为 ¥X」。 */
    public String observation();
}
```

口径来源：既有 [`MarketAnalysisTool.summarize`](../easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/adapter/outbound/tool/MarketAnalysisTool.java) 已经在搜索管道上做同一件事，但吃的是 `ProductReadModel`（搜索管道专用类型）。agent 侧攒的是 `AssetHit`，所以是**照口径重写，不是复制粘贴**，也不要为了复用去改 `MarketAnalysisTool`。

## 四、A 必须遵守的约定（全部可验证）

1. **domain 层纯度**（ArchUnit Rule 1，[ArchitectureRulesTest.java:56](../easyorange-backend/easyorange-application/src/test/java/com/cartethyia/easyorange/architecture/ArchitectureRulesTest.java)）：`..domain..` 只允许依赖 `java..` / `jakarta.annotation..` / `lombok..` / `org.slf4j..` / `org.jetbrains.annotations..` / MyBatis-Plus 注解 / Jackson 注解 / `com.cartethyia.easyorange.common..` / 本仓库 `..domain..`。**新类只用 JDK**——不引 Spring、不引 jspecify（`AssetDetail` 用了 `org.jspecify.annotations.Nullable` 是既存类，新类不要跟着学，白名单里没有它），空值一律显式判空。
2. **纯计算类不抛业务异常**：非法输入返回 `Optional.empty()`，异常语义由接线层负责（同 `AssetDetailPort.findDetail` 的 empty 约定）。也别 `throw new IllegalArgumentException` 表达「输入不足」——那是 normal case。
3. **金额展示**：`BigDecimal.stripTrailingZeros().toPlainString()`（`¥4200` 而不是 `¥4200.00`）。
4. **测试风格**（照 `RrfFusionTest`）：JUnit 5 + AssertJ；类上 `@DisplayName("<类名> -> 测试")`；方法名 `方法_场景`；边界必须覆盖空列表 / 单元素 / 全部字段为 null / 未知 ID 混入 / 面议（price=null）参与比较。
5. **注释说约束不复述代码**，中文，仓库既有风格。

## 五、A 的验收命令

```bash
cd easyorange-backend
./mvnw test -pl easyorange-ai -Dtest='AssetComparisonTest,PriceStatsTest'   # 快速循环
./mvnw test -pl easyorange-ai                                              # 模块全绿
```

ArchUnit 规则在 `easyorange-application` 模块、跨模块编译很贵，**不进 A 的循环**，由 B 收口时跑一次：

```bash
cd easyorange-backend && ./mvnw test -pl easyorange-application -Dtest='ArchitectureRulesTest'
```

## 六、A 明确不做

- 不碰 `AgentTools` / `AgentLoopRunner` / `ai_chat_tool.yml` / `application.yaml` / 任何 `doc/` 下文件
- 不注册 Spring bean、不写 Port、不加缓存、不加 Micrometer 指标
- 不做 rerank、不做多智能体编排（后者归第二个项目）
- 不自行决定工具名、工具参数与注册方式——那些是 B 的接线决定

## 七、B 的清单（本会话）

1. 把 `AssetComparison` / `PriceStats` 接进 `AgentTools`：补 `@Tool` 声明、参数校验（ID 必须来自此前观察）、观察文本格式化、未知 ID 的失败观察合成
2. `remember_preference` 拆出、`finish` 瘦身、`max-steps` 5 → 7
3. `ai_chat_tool.yml` 规则重写：引导模型先检索再计算、缺信息时补检索
4. `AgentLoopRunnerTest` 适配；SSE step 协议与 `eo_agent_step_trace` 字段语义不变
5. 文档校准：面试文档里的失实表述与工程指标残留数字（清单在 B 的会话里，不在本文）
6. 收口：ArchUnit + ai 模块全绿 + `python3 .githooks/check-metrics-drift.py --fix`
