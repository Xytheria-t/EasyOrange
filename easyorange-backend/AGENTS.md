# easyorange-backend — 后端约定与踩坑

> Maven 多模块（`common` / `framework` / 业务模块 + 组装 `easyorange-application`），模块清单见[结构计数](../doc/工程指标.md#结构计数)。全局硬约束见[根 AGENTS.md](../AGENTS.md)。只写读代码看不出来的约定与踩坑。

## 构建 / 启动 / 环境

- **改子模块后启动前必须 `./mvnw install -DskipTests`**（或 `clean package -pl <module> -am`），否则按旧 jar 编译、接口改动**假失败**
- 启动统一 `./mvnw spring-boot:run -pl easyorange-application`；IDE 调试需手动把 pom `<jvmArguments>` 补进 VM options（不传给 IDE）；禁止终端 / IDE 混跑
- 新增子模块须在父 POM `<modules>` 注册；依赖版本以 `pom.xml` 为单一来源
- **环境变量单一来源是根 `.env`**（键位看 `.env.example`）：compose 插值 / 终端 `set -a; source .env; set +a` / IDEA 默认不读，需 Run Configuration 或 EnvFile
- **占位符语法**：`application*.yaml` 用 `${VAR:default}`；`compose.yaml` / shell 用 `${VAR:-default}`——YAML 误写 `:-` 把 `-default` 当字面量（Redis 密码 → WRONGPASS）
- 仅 prod 读 `SERVER_PORT` / `TRACING_SAMPLING_PROBABILITY`；压测关限流 `RATE_LIMIT_FILTER_ENABLED=false`；AI base-url / model 硬编码在 `application.yaml`
- **dev seed 账号**明文统一 `Password123`（`db/dev/R__seed_dev_test_data.sql`）；登录失败 5 次锁 30 分钟，清 `eo:user:login:attempts:<identifier>` 解锁
- 启动的 Unsafe WARNING 已配 `--sun-misc-unsafe-memory-access=allow` 抑制（Lombok 仍用 Unsafe）；**JDK 26+ 默认 deny，届时升级 Lombok**

## 命名与事务

- **查询方法必须 `@Transactional(readOnly = true)`**（find/get/list/query/count）；写操作 `rollbackFor = Exception.class`
- 后缀：事件 `*Event`、应用服务 `*AppService`、CQRS `*CommandHandler` / `*QueryHandler`、出站端口 `*Port`（`domain/port/`）；写仓储 `*Repository`、读仓储 `*QueryRepository`（`application/port/query/`，**读模型是 application 层概念**）
- **命令入口一命令一方法**（用例动词短语，不用重载 / `handle`）；**Command 顶层 record 一命令一文件**，禁 inner record、禁 `@Data` / `@Builder`；`sealed interface` 只在有穷尽分发消费者时引入
- **一事务只改一个聚合根**；聚合间只按 ID 引用；状态机只给真有生命周期的聚合
- **领域服务不用 `@Service`**，模块根 `config/` 手动注册；domain 禁 Spring 注解（Rule 1）、application 装配 adapter 被 Rule 7 拦 → **模块根 `config/` 是组装根唯一合法位**，不收非配置类
- 归属：聚合根「我变我自己」/ 领域服务「我帮你们协调」/ 应用服务「我负责跑腿」；**应用服务按依赖集与事务边界聚合**（构造函数变更不应强迫改不相关调用方）
- 常量紧贴使用者所在层（业务 `domain/enums`、共享 `common/enums`、技术常量适配器层）；枚举包复数 `enums/`（关键字）、常量包单数
- **服务返回值**：创建返回 `String` ID；update / 命令返回 `void`（前端 invalidate 重拉）；批量可返回结果 DTO
- Controller 无转换 / 条件逻辑时 `Result.success()` 内联单表达式；`common` 禁引 Starter / 重量级依赖，`FileException` 用 `.of(...)`，`BaseCodeEnum.fromCode` 未匹配 fail-fast

## DTO / 类型 / 序列化

- **请求 DTO 一律 record**（Jackson 3 构造器绑定），默认值放紧凑构造器；例外：`PageRequest` 子类与 `WsMessage`；**嵌套 DTO 列表必须 `@Valid` 级联**
- **DTO 分层**：application 与 adapter 共用的 Response 必须放 `application/dto/`（放 adapter 违反依赖倒置）；仅 controller 单用的可留 adapter
- **`BaseDO`**：`IdType.INPUT` String id、createTime / updateTime 走 `FieldFill`、`@TableLogic`；**`version` 乐观锁按需加**（ProductDO / OrderDO / PaymentDO）
- ID 统一 String（UUID v7，36 字符，列 `CHAR(36)`），MyBatis-Plus 无需 UUID TypeHandler
- **Jackson 3**：`JacksonException`、包 `tools.jackson.*`；模块需**显式加 `jackson-core`**（不传递）；事件 record 无需 `@JsonCreator`；`ToStringSerializer` **`Long` 与 `long` 都注册**；不配 Jackson 2 `ObjectMapper`、`WebMvcConfig` 不重写 `extendMessageConverters`（Jackson 2 配已无效）
- **不可变集合统一 `List.of()` / `copyOf` 系**，禁 `Collections` 工具类；取值面固定的字段用枚举不用 String（非法值绑定期即失败）
- **Mapper IN 查询必须 `<script>` + `<foreach>` 参数化**，禁 `${}` / JSON / CSV 拼接（历史事故 `IN (${ids})`：注入 + 阻止计划缓存）

## 配置

- 配置属性统一 **record + `@ConfigurationProperties` + scan**：禁散落 `@Value`、类上不加 `@Component`；标量 / `Duration` 用 `@DefaultValue`，集合与嵌套在紧凑构造器兜底；`@Validated` fail-fast、嵌套需父组件 `@Valid`；**null 兜底只能靠紧凑构造器**
- 业务模块由 scan 统一注册，**别把 `@EnableConfigurationProperties` 挂到适配器等组件上**
- **框架配置类 = `@AutoConfiguration` + `AutoConfiguration.imports` 加一行**，不靠隐式扫描
- **yml 里 `x:y` 形值必须加引号**：SnakeYAML 裸 `1:1` 按六十进制解析成 61（已踩 `default-aspect-ratio`）
- `easyorange.cache.default-ttl` 默认 30m；`thread-pool.*` / `idgen.*` / `http-client.*` 已删（虚拟线程仅 `taskScheduler` poolSize=5）

## 缓存与 Redis

- **`RedisConfig` 必须显式配序列化器**：Boot 4 默认 `JdkSerializationRedisSerializer`（二进制 key，Lua `tonumber(ARGV)` → nil）。注入 `RedisTemplate<Object, Object>`、`@AutoConfigureBefore(DataRedisAutoConfiguration)`，key 用 `StringRedisSerializer`、value 用 `GenericJacksonJsonRedisSerializer.builder().enableDefaultTyping(...)`；**禁自定义 `RedisTemplate<String, Object>`**。不配 → 限流 Lua ARGV 二进制 → **限流器 fail-open**
- **缓存值必须是 POJO / record 或 `ArrayList`**：`java.*` final 类型（`Optional`、`List.of()`）无类型信息**无法反序列化**
- **新增 Redis 存储形状必须补 roundtrip 断言**（`RedisJsonSerializerRoundtripTest` / `CacheSerializerRoundTripTest`），序列化器改动先过这些断言
- 防穿透靠缓存 null：**不加 `unless = "#result == null"`**；列表缓存 `orEmpty` 兜可变空列表
- Key 形如 `eo:模块:业务:标识`；`CacheErrorHandler` 统一 fail-open（读直查 DB / 写放弃），**不逐点包熔断**

## 安全 / 过滤器链

- **JWT 走 OAuth2 Resource Server 内置 Filter，无自定义认证 Filter**：`JwtDecoder` 只验签 + issuer；`JwtAuthenticationConverter` 拒 refresh token、从 `authorities` claim 构造 `AuthUser`；RSA 2048（生产 `jwt.*-key-location` PEM）
- 管理员角色在 `login()` 写进 JWT claim，资源服务器直接读、**不重算**
- **双 Token**：Access JWT 30 分钟（前端仅内存）；Refresh opaque 落 Redis（HttpOnly Cookie，key `eo:user:refresh:*`，SHA-256），**轮换 + 复用检测**（`RefreshTokenStore`）
- 登出 jti 进黑名单（TTL = 剩余有效期）；`TokenRevocationFilter` 只查吊销（与验签职责分离）；`WebSocketAuthInterceptor` 复用同一 `JwtDecoder`
- **Filter 顺序**：`RateLimitFilter(0)` → `RefreshCsrfFilter(1)` → resource server 内置认证 → `TokenRevocationFilter` → Anonymous → `AuditLogAspect`（`@Order 3`）
- **`RateLimitFilter` 必须 `ObjectProvider<List<HandlerMapping>>` 延迟注入**：直接注入经 WebSocket 配置链**循环依赖**，别改回 `@RequiredArgsConstructor`
- 限流：GET 本地 200/60s/IP；写 Redis 30/60s/IP；fail-open；`@SkipRateLimit` 跳过。防重：拦 POST/PUT/DELETE/PATCH（3s 间隔，key 含 body hash，不缓存响应）；`@SkipRepeatSubmit` 跳过
- **幂等 ≠ 防重**：`IdempotencyKeyFilter` 24h 窗口 + `Idempotency-Key` 头 + **字节级回放响应**（非 2xx 不缓存），置于 `AnonymousAuthenticationFilter` 前抓最终响应
- 密码 BCrypt；CORS 生产白名单；审计日志约定式记录写操作（异步 + 掩码）
- **`security.product-paths` 前缀匹配陷阱**：`/api/products` 会匹配 `/api/products/my`——新增需认证接口补精确 `.requestMatchers(...).authenticated()`
- **登录失败统一「用户名或密码错误」**（防用户枚举）；账号禁用可单独提示

## 事件与 MQ

- **领域事件只注入 `DomainEventPublisher.publish()`**：`ModulithDomainEventPublisher`（`@Primary`）→ Spring Modulith 持久化 `EVENT_PUBLICATION`（**与事务同原子 = Outbox**）→ 提交后异步发 `eo.domain.events`；`matchIfMissing = true` 支持无 RabbitMQ 启动；路由键按类名派生
- **消费者统一 `@RabbitListener` + `EventConsumerHandler.handle(...)`**：封装幂等（Redis `SET NX EX` + 24h TTL，失败 unmark 放行重投）/ 元数据 / 指标 / DLQ 四横切——at-least-once + 幂等 = 精确一次；`idempotencyEnabled=false` 关投影 / 广播类消费者
- AMQP：concurrency 用 `concurrent-consumers` + `max-concurrent-consumers`（**不支持 `"1-5"`**）；**不要按商品事件触发 LLM**（旧实现纯浪费，已删）

## 测试

- Mockito 用 `mock-maker-subclass`，**别改回 inline**（WSL2 ByteBuddy attach 失败）
- **集成测试不用 Testcontainers**：`*IT` 继承 `AbstractIntegrationTest`，`spring-boot-docker-compose` 复用根 compose、failsafe `mvn verify`；ryuk 拉取失败会**静默跳过**（TD-001）
- `application-it.yaml` 复用 dev 栈显式 localhost、**关 Flyway 校验**
- **record 不能被 Mockito mock**：用 `testsupport/PropertyBindings` 真实 Binder 构造
- Boot 4 `@WebMvcTest` 在 `org.springframework.boot.webmvc.test` 包；无 `@SpringBootConfiguration` 建空启动类；`@ComponentScan` 限 controller 包否则切片失败
- AI 测试：mock `ChatModel` / `EmbeddingModel`，协作者 mock 端口；`argThat` null-safe
- **`ArchitectureRulesTest` 白名单已清零，禁新增**
- **Flyway**：`V{N}__description.sql`，DDL `db/migration/`、开发数据 `db/dev/`；**禁改已执行脚本**；**新字段必须可空或有默认值**；不写业务逻辑

## 模块要点

### order

- **拒绝 Saga（ADR-0007）**：本地单事务 + 分布式锁 + Outbox，无反向补偿。下单：锁**事务外获取、提交后释放**（key `eo:order:lock:product:{id}` 按 id 排序防死锁、等 10s），批量读齐快照，扣库存**订单 ID 即幂等键**，任一步失败整体回滚（B3009）
- **PAID 唯一来源是 payment 事件**（`PUT /pay` 经网关两阶段，禁直接置位）；库存恢复仅由 `OrderLifecycleEventConsumer` 按事件明细对称恢复（**空明细 = 载荷损坏显性失败**）
- **`OrderAction` 是状态机唯一事实来源**，`transitionTo` 统一守卫**禁绕过**；新增转换必须 **Flyway 给 status CHECK 追加 code**；**会产生事件的写操作必须加载行项**（否则事件明细空 → 库存恢复 / 售出标记**静默失效**）；订单项自持快照 `product_snapshot`，**读侧不跨模块查商品**

### payment

- **两阶段状态机（不可变聚合根 + `Transition<Payment, E>` 返回新实例）**：`preparePay` → 网关 → `confirmPay`，退款同型，**不跨服务编排**；`PaymentPhaseExecutor` 独立 Bean 保 `@Transactional` 生效
- **枚举全链路 String code**（DB `VARCHAR(20)` + CHECK，`@JsonValue` 在 code 上）；新增支付方式 = 枚举 + 网关逻辑 + **Flyway 给 `payment_method` CHECK 追加 code**；**回调必须 HMAC-SHA256 验签**
- 模块无幂等表（协议级走 framework `IdempotencyKeyFilter`）；支付 / 退款经 `DistributedLockPort` **等待 0 秒**，争用 → `PAYMENT_BUSY` 429 网关重试兜底

### product

- **库存并发三层**：分布式锁管「同时写」、`@Version` 管丢失更新（B0006）、流水 `eo_stock_ledger` 管「几次多少」——任何库存变更**同事务落 `StockChange`**（唯一索引幂等），对账日比对**告警不改余额**，`StockQuantity` 扣负即抛（超卖最后防线）；**绕开聚合根的单列 SQL 更新直接触发对账漂移告警**
- 商品事件 `implements ProductEvent`（密封接口派生 `aggregateId()`）；同步副作用同事务、异步投影走队列 `eo.product.cqrs`
- 缓存端口分拆：domain 只驱逐、application `get(id, loader)`（**null 不落缓存**）、adapter 同时实现；**本模块是端口定义方**（`ProductInventoryPort` / `ProductSearchQueryPort` / `AiSearchEnhancerPort`），ai / order 的实现都在 `easyorange-application/adapter/outbound/`

### user

- **登录每方式独立 DTO + 独立端点**，`toCredential()` → `LoginCredential`（sealed：`Password` / `Sms`），**禁枚举字段区分**；两层映射：`UserEntityMapper` 管全部持久化（`UserDO` 无 `toDomain()`）、`UserAssembler` 管响应脱敏
- `SmsCodePort` / `SmsSenderPort` **按 `@Profile` 互斥**，未声明 profile 不注册、**启动即失败防裸跑生产**；validation 包只放**纯格式**校验，唯一性在 application / domain
- **改密码成功必须吊销全部会话 + 发 `UserPasswordChangedEvent`**，新旧同密码拒；注册 `nick_name` **默认 = `username`**；新增字段走值对象 record → Flyway → DO/Mapper → DTO/Assembler → 聚合根

### message

- **STOMP over WebSocket**：`WebSocketAuthInterceptor` 从 STOMP Header 提 JWT；聊天帧 `/queue/chat/{conversationId}`、未读 `/queue/unread-count`；离线先落 PENDING、上线补推
- **REST 与 WebSocket 必须共用 `MessageCommandHandler`**——限流唯一裁决点防双重计数；发送前敏感词过滤；限流 5 条/秒/用户；**XSS 在渲染端 `escapeHtml`，聚合根不转义**；conversationId = `conv_{minId}_{maxId}`

### ai

- **按能力分包** `chat` / `retrieval` / `enhancement` / `listing` / `support` / `eval`（单向，`support` 最底层）；**全面 Spring AI（ADR-0008）**：直接注入 `ChatModel` / `EmbeddingModel`，无自研 LlmPort
- 模型 bean：`chatModel`（`@Primary`）、`visionChatModel`（`@Qualifier`）、`embeddingModel`（**dimensions=1024 必须与 ES `dense_vector` 对齐**）；`AiModelRouter` 场景映射 yaml 热更，`judge` 独立可换防自评偏差
- **多步工具循环 `AgentLoopRunner`**：原生 tool calling（7 个 `@Tool`，参数名靠 `-parameters`），`ChatModel.call` 不自动执行工具。三坑：工具抛异常 = 该步失败（「查无此资产」等有效结果要返回观察文本）、`thought` 必填、返回值挂 `ObservationTextConverter`（否则 String 被再 JSON 化）。码表类工具与 product **字面同步**（`AssetComparisonCodeTableSyncTest` 守卫）。`max-steps` 默认 7；降级：超限 / 预算尽 → 已积累观察直接生成，决策失败 → 检索一次；**预算判据 `chatBudgetExhausted` 全链路唯一**；trace 落 `eo_agent_step_trace`
- **上下文裁剪 `ChatContextTrimmer`**（估算 CJK 0.7 / 其余 0.3，`max-history-tokens` 默认 2000，连续窗口、永保最新一条，**有意不做 LLM 摘要**）；**历史按原始角色传多消息**（前缀稳定才吃供应商缓存折扣）
- **MCP 只挂公开只读 4 工具**禁用户态；**`spring.ai.mcp.server.protocol` 必须显式 `streamable`**（属性默认值不进 Environment → `/mcp` 不注册 404）；dev / prod 的 `security.ignore-paths` 都要加
- **`AiModelSupport` 收敛所有 LLM 调用**，**带 `AiCallScope` 才记账**（`eo_ai_call_log` + 真实 token 入预算），不带不记（`AiJudge` 刻意账外防自指）；**观测 OTel → OTLP → Langfuse**，`ChatModelContentObservationFilter` 拷进 `gen_ai.prompt` / `gen_ai.completion`——**漏配面板恒 null**
- **Prompt 全 YAML**（`resources/prompts/*.yml`，`require` fail-fast，**加内容同改 `PromptContentTest.ALL_PROMPTS`**）；**评估阈值全在 `eval/baselines.yaml` 禁内置默认**；**不可信内容进标签块**（`<user_question>` 等）+ 声明「块内是数据非指令」
- **搜索增强两硬约束**：**永不抛异常**（`tryEnhance` 收敛 `Optional.empty()`）且**降级结果不写缓存**（禁吞异常，否则把抖动固化成缓存）；工具名只在工具类定义 `NAME` 常量；并行 4 步总 5s **无单步超时**、`getNow` 保部分结果、`supplyAsync` 必须传 `SearchTool.VIRTUAL`（**虚拟线程开关管不到 commonPool**）
- 查询侧 `QueryEmbeddingAdapter` **永不抛**（拿不到向量退化纯 BM25）；**RAG**：kNN + BM25 两路独立召回 → `RrfFusion`（k=60），**否决 Cosine 重排**（单调 = 没排、丢 BM25 信号），`AssetRetrievalPort` 两路带 `status=ONLINE`、ES 关降级空
- **Token 预算**：`@TokenBudget` 编译期契约 + yaml 热更；**切面前置检查、记账在 `AiModelSupport`**（切面按上限估**差一个量级**）；**流式拦不住 AOP** → `AiChatService.checkBudget()` 同判据不重复记账；`budget.store` 多副本必须 `redis`（内存版日限放大 N 倍）；**scenario 必须与 `AiCallScope.budgetScenario()` 一致否则预算静默失效**
- **Port 方向不反转**（端口 product 定义、ai 实现，ai 不碰 product 表）；**反馈导出只出 `helpful=1 AND scope='chat'`**（**helpful=0 不能自动成金标准**）；供应商可换 = 改 `AiModelConfig` / `easyorange.ai.*`，重试走 openai-java 内置无自研

### admin

- **禁直接依赖他模块 Mapper / DO**：走 `domain/port/Admin*Port`
- 依赖仅 optional `common` + `framework`，**其余业务模块零依赖**；写操作记 reason + 操作人

## IDE 误报

- IntelliJ 把 domain port 识别为 Spring Bean → **Adapter 实现类加 `@Primary`**
- `@Mapper(componentModel = "spring")` 同理 → 构造器注入加 `@Qualifier`，字段注入加 `@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")`
