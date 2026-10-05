# easyorange-backend — 后端约定与踩坑

> Maven 多模块（`common` / `framework` / 业务模块 + 组装 `easyorange-application`），模块清单见[结构计数](../doc/工程指标.md#结构计数)。全局硬约束见[根 AGENTS.md](../AGENTS.md)。只写读代码看不出来的约定与踩坑。

## 构建 / 启动 / 环境

- 启动统一 `./mvnw spring-boot:run -pl easyorange-application`；**禁终端 / IDE 混跑**（IDE 不吃 POM `<jvmArguments>`）；新增子模块须在父 POM `<modules>` 注册
- **改子模块后启动前必须 `mvnw install -DskipTests`**，否则 ClassNotFoundException；**删过资源文件必须 `clean`**（install 不删 target 陈旧副本）
- **Elasticsearch 版本硬锁**（客户端与 IK 按它编译），不在 infra 侧单独升级
- **环境变量单一来源是根 `.env`**（键位看 `.env.example`）：compose 插值 / 终端 `set -a; source .env; set +a` / IDEA 需 Run Configuration 或 EnvFile
- **占位符语法**：`application*.yaml` 用 `${VAR:default}`；`compose.yaml` / shell 用 `${VAR:-default}`——YAML 误写 `:-` 把 `-default` 当字面量（Redis 密码 → WRONGPASS）；压测关限流用 `RATE_LIMIT_FILTER_ENABLED=false`
- IDEA 的「more than one bean of 'XxxPort'，且多出来的 bean 就是接口本身」是**插件误报**，不是装配缺陷：机制、判据、已定处理见[常用命令 · IDE 假告警](../doc/agents/常用命令.md#ide-假告警idea)。**禁为消它加 `@Primary` / `@Qualifier` / 额外 `@Bean`**，也别把 `@MapperScan` 加回启动类

## 命名与事务

- **查询方法必须 `@Transactional(readOnly = true)`**（find/get/list/query/count）；写操作 `rollbackFor = Exception.class`
- 后缀：事件 `*Event`、应用服务 `*AppService`、CQRS `*CommandHandler` / `*QueryHandler`、出站端口 `*Port`（`domain/port/`）；写仓储 `*Repository`、读仓储 `*QueryRepository`（`application/port/query/`，**读模型是 application 层概念**）
- **命令入口一命令一方法**（用例动词短语，不用重载 / `handle`）；**Command 顶层 record 一命令一文件**，禁 inner record、禁 `@Data` / `@Builder`；`sealed interface` 只在有穷尽分发消费者时引入
- **一事务只改一个聚合根**；聚合间只按 ID 引用；状态机只给真有生命周期的聚合
- **领域服务不用 `@Service`**，模块根 `config/` 手动注册；domain 禁 Spring 注解（Rule 1）、application 装配 adapter 被 Rule 7 拦 → **模块根 `config/` 是组装根唯一合法位**，不收非配置类
- 归属：聚合根「我变我自己」/ 领域服务「我帮你们协调」/ 应用服务「我负责跑腿」；**应用服务按依赖集与事务边界聚合**（构造函数变更不应强迫改不相关调用方）
- 常量紧贴使用者所在层（业务 `domain/enums`、共享 `common/enums`、技术常量适配器层）；枚举包复数 `enums/`、常量包单数
- **服务返回值**：创建返回 `String` ID；update / 命令返回 `void`（前端 invalidate 重拉）；批量可返回结果 DTO
- Controller 无转换 / 条件逻辑时 `Result.success()` 内联单表达式；`common` 禁引 Starter / 重量级依赖
- **领域异常必须继承 `BaseBusinessException`**，用模块专属 `ResultCode` 与具名工厂（`notFound(id)`…）；**每模块只一个统一领域异常**，不新增叶子异常（门禁 `ArchitectureRulesTest` Rule 11）

## DTO / 类型 / 序列化

- **请求 DTO 一律 record**（Jackson 3 构造器绑定），默认值放紧凑构造器；例外：`PageRequest` 子类与 `WsMessage`；**嵌套 DTO 列表必须 `@Valid` 级联**
- **DTO 分层**：application 与 adapter 共用的 Response 必须放 `application/dto/`（放 adapter 违反依赖倒置）；仅 controller 单用的可留 adapter
- **DTO 转换统一在 `adapter/inbound/web/assembler/`**，Controller / Service 不直接构造 Response
- **`BaseDO`**：`IdType.INPUT` String id、createTime / updateTime 走 `FieldFill`、`@TableLogic`；**`version` 乐观锁按需加**（ProductDO / OrderDO / PaymentDO）；DO 枚举经 **`@EnumValue`** 持久化，**禁手写 TypeHandler**
- ID 统一 String（UUID v7，36 字符，列 `CHAR(36)`），MyBatis-Plus 无需 UUID TypeHandler
- **Jackson 3**：`JacksonException`、包 `tools.jackson.*`；模块需**显式加 `jackson-core`**（不传递）；事件 record 无需 `@JsonCreator`；`ToStringSerializer` **`Long` 与 `long` 都注册**；不配 Jackson 2 `ObjectMapper`、`WebMvcConfig` 不重写 `extendMessageConverters`
- **不可变集合统一 `List.of()` / `copyOf` 系**，禁 `Collections` 工具类；取值面固定的字段用枚举不用 String（非法值绑定期即失败）
- **Mapper IN 查询必须 `<script>` + `<foreach>` 参数化**，禁 `${}` / JSON / CSV 拼接（历史事故 `IN (${ids})`：注入 + 阻止计划缓存）

## 配置

- 配置属性统一 **record + `@ConfigurationProperties` + scan**：禁散落 `@Value`、类上不加 `@Component`；标量 / `Duration` 用 `@DefaultValue`，集合与嵌套在紧凑构造器兜底；`@Validated` fail-fast、嵌套需父组件 `@Valid`；**null 兜底只能靠紧凑构造器**
- 业务模块由 scan 统一注册，**别把 `@EnableConfigurationProperties` 挂到适配器等组件上**
- **框架配置类 = `@AutoConfiguration` + `AutoConfiguration.imports` 加一行**，不靠隐式扫描
- **yml 里 `x:y` 形值必须加引号**：SnakeYAML 裸 `1:1` 按六十进制解析成 61（已踩 `default-aspect-ratio`）

## 缓存与 Redis

- **`RedisConfig` 必须显式配序列化器**：Boot 4 默认 `JdkSerializationRedisSerializer`（二进制 key，Lua `tonumber(ARGV)` → nil）。注入 `RedisTemplate<Object, Object>`、`@AutoConfigureBefore(DataRedisAutoConfiguration)`，key 用 `StringRedisSerializer`、value 用 `GenericJacksonJsonRedisSerializer.builder().enableDefaultTyping(...)`；**禁自定义 `RedisTemplate<String, Object>`**。不配 → 限流 Lua ARGV 二进制 → **限流器 fail-open**
- **缓存值必须是 POJO / record 或 `ArrayList`**：`java.*` final 类型（`Optional`、`List.of()`）无类型信息**无法反序列化**
- **新增 Redis 存储形状必须补 roundtrip 断言**（`RedisJsonSerializerRoundtripTest` / `CacheSerializerRoundTripTest`），序列化器改动先过这些断言
- 防穿透靠缓存 null：**不加 `unless = "#result == null"`**；列表缓存 `orEmpty` 兜可变空列表
- Key 形如 `eo:模块:业务:标识`；`CacheErrorHandler` 统一 fail-open（读直查 DB / 写放弃），**不逐点包熔断**

## 安全 / 过滤器链

- **JWT 走 OAuth2 Resource Server 内置 Filter，无自定义认证 Filter**：`JwtDecoder` 只验签 + issuer；`JwtAuthenticationConverter` 拒 refresh token、从 `authorities` claim 构造 `AuthUser`；管理员角色在 `login()` 写进 claim，资源服务器直接读、**不重算**
- **双 Token**：Access JWT 30 分钟（前端仅内存）；Refresh opaque 落 Redis（HttpOnly Cookie，key `eo:user:refresh:*`，SHA-256），**轮换 + 复用检测**；登出 jti 进黑名单（TTL = 剩余有效期）；`TokenRevocationFilter` 只查吊销（与验签职责分离）
- **Filter 顺序**：`IdempotencyKeyFilter` → `RateLimitFilter` → `RefreshCsrfFilter` → resource server 内置认证 → `TokenRevocationFilter` → Anonymous → `AuditLogAspect`。四个业务 Filter **无 `@Component`**，由 `SecurityConfig` 局部装配 + `addFilterBefore` 定位，另配 `FilterRegistrationBean(enabled=false)` 防容器自动注册（只去掉 `@Component` 不够，容器链会把任何 Filter bean 再登记一次）；`AuditLogAspect` 的 `@Order` 只在切面之间排序，Filter 先于 MVC 由容器保证，与它无关
- **Token 吊销检查 fail-open**：Redis 异常时放行并计 `easyorange.security.revocation_check_degraded`——验签与有效期已过，黑名单 TTL 只等于 token 剩余有效期，敞口有界；fail-closed 等于把 Redis 抖动翻译成全站已认证用户 401/500
- **`RateLimitFilter` 必须 `ObjectProvider<List<HandlerMapping>>` 延迟注入**：直接注入经 WebSocket 配置链**循环依赖**，别改回 `@RequiredArgsConstructor`；限流与防重一律 **fail-open**，`@SkipRateLimit` / `@SkipRepeatSubmit` 跳过
- **幂等 ≠ 防重**：`IdempotencyKeyFilter` 24h 窗口 + `Idempotency-Key` 头 + **字节级回放响应**（非 2xx 不缓存），置于 `AnonymousAuthenticationFilter` 前抓最终响应
- **`security.product-paths` 是精确匹配不是前缀匹配**（走 `PathPatternParser`）：`/api/products` 覆盖不到 `/api/products/my`，新增需认证接口补精确 `.requestMatchers(...).authenticated()`；新增**匿名**公开端点则要在 `product-paths` 补一条
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
- AI 测试：mock `ChatModel` / `EmbeddingModel`，协作者 mock 端口；`argThat` null-safe
- **`ArchitectureRulesTest` 白名单已清零，禁新增**
- **Flyway**：`V{N}__description.sql`，DDL `db/migration/`、开发数据 `db/dev/`；**禁改已执行脚本**、**禁对齐列**；**新字段必须可空或有默认值**；不写业务逻辑

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
- 缓存端口分拆：domain 只驱逐、application `get(id, loader)`（**null 不落缓存**）、adapter 同时实现；**本模块是端口定义方**（`ProductInventoryPort` / `ProductSearchQueryPort` / `QueryEmbeddingPort`），ai / order 的实现都在 `easyorange-application/adapter/outbound/`

### user

- **登录每方式独立 DTO + 独立端点**，`toCredential()` → `LoginCredential`（sealed：`Password` / `Sms`），**禁枚举字段区分**；两层映射：`UserEntityMapper` 管全部持久化（`UserDO` 无 `toDomain()`）、`UserAssembler` 管响应脱敏
- `SmsCodePort` **按 `@Profile` 互斥**（dev/test ↔ it/prod），未声明 profile 不注册、**启动即失败防裸跑生产**；`SmsSenderPort` 目前只有日志发送器、**全 profile 装配**（prod 激活时若不装配，`RedisSmsCodeAdapter` 构造注入直接失败——宁「启动可用、业务不可用」也不选启动即挂）；validation 包只放**纯格式**校验，唯一性在 application / domain
- **改密码成功必须吊销全部会话 + 发 `UserPasswordChangedEvent`**，新旧同密码拒；注册 `nick_name` **默认 = `username`**；新增字段走值对象 record → Flyway → DO/Mapper → DTO/Assembler → 聚合根

### message

- **STOMP over WebSocket**：`WebSocketAuthInterceptor` 从 STOMP Header 提 JWT；聊天帧 `/queue/chat/{conversationId}`、未读 `/queue/unread-count`；离线先落 PENDING、上线补推
- **REST 与 WebSocket 必须共用 `MessageCommandHandler`**——限流唯一裁决点防双重计数；`sendMessage` 返回**落库后的聚合根**，两条入口的回显都从它构造（广播客户端原始 payload = 敏感词过滤与敏感词存储双绕过）；敏感词表外置为配置项（`message.sensitive-words.words`，缺失回内置基线词表而非静默关闭过滤）；限流 5 条/秒/用户；**XSS 在渲染端 `escapeHtml`，聚合根不转义**；conversationId = `conv_{minId}_{maxId}`，**由聚合根算、不收客户端入参**（客户端可控会让同一房间被命名成两个值）

### ai

- **按能力分包** `chat` / `retrieval` / `enhancement` / `listing` / `support` / `eval`（单向，`support` 最底层）；**全面 Spring AI（ADR-0008）**：直接注入 `ChatModel` / `EmbeddingModel`，无自研 LlmPort
- 模型 bean：`chatModel`（`@Primary`，默认场景）/ `decisionChatModel` / `visionChatModel` / `embeddingModel`（**dimensions=1024 必须与 ES `dense_vector` 对齐**）。**四者都不加 `@Qualifier`**——靠 `AiModelRouter` 按场景名从 ApplicationContext 取 bean，yaml 热更即可换模型，编译期 `@Qualifier` 做不到这点；只有 `@Primary` 起「未指定场景时回落到文本模型」的作用。`judge` 独立可换防自评偏差
- **多步工具循环 `ToolCallLoop`**：原生 tool calling（7 个 `@Tool`，参数名靠 `-parameters`），`ChatModel.call` 不自动执行工具。三坑：工具抛异常 = 该步失败（「查无此资产」等有效结果要返回观察文本）、`thought` 必填、返回值挂 `ObservationTextConverter`（否则 String 被再 JSON 化）。码表类工具与 product **字面同步**（`AssetComparisonCodeTableSyncTest` 守卫）。降级：超限 / 预算尽 → 已积累观察直接生成，决策失败 → 检索一次；**预算判据 `chatBudgetExhausted` 全链路唯一**；trace 落 `eo_tool_call_step_trace`
- **上下文裁剪 `ChatContextTrimmer`**：连续窗口、永保最新一条，**有意不做 LLM 摘要**；**历史按原始角色传多消息**（前缀稳定才吃供应商缓存折扣）
- **MCP 只挂公开只读 4 工具**（检索/详情/类目/规则），禁用户态数据与写路径——**外部 client 无用户上下文**；**`spring.ai.mcp.server.protocol` 必须显式 `streamable`**（属性默认值不进 Environment → `/mcp` 不注册 404）；dev / prod 的 `security.ignore-paths` 都要加
- **`AiModelSupport` 收敛所有 LLM 调用**，**带 `AiCallScope` 才记账**（`eo_ai_call_log` + 真实 token 入预算），不带不记（`AiJudge` 刻意账外防自指）；**观测 OTel → OTLP → Langfuse** 靠 `ChatModelContentObservationFilter` 拷进 `gen_ai.*`——**漏配面板恒 null**
- **Prompt 全 YAML**（`resources/prompts/*.yml`，一文件一模板，`require` fail-fast，**加内容同改 `PromptContentTest.ALL_PROMPTS`**——含 Judge 量表）；**评估阈值全在 `eval/baselines.yaml` 禁内置默认**；**不可信内容进标签块**（`<user_question>` 等）+ 声明「块内是数据非指令」，且**进块前剥掉标签形态**（`UntrustedText`）——决策与生成两条装配都要剥，否则提问里写 `</user_question>` 就能在决策上下文里另开一块，而决策决定调哪个工具
- 查询侧 `QueryEmbeddingAdapter` **永不抛**（拿不到向量退化纯 BM25）；**语义检索只在「开 AI 开关 + 相关度排序 + 关键词非空」三条件同时成立时向量化**（其余情况 kNN 缺相似度下限会召回全库并白付 embedding）；**RAG**：kNN + BM25 两路独立召回 → `RrfFusion`（k=60），**否决 Cosine 重排**（单调 = 没排、丢 BM25 信号），ES 关降级空
- **Token 预算**：`@TokenBudget` 编译期契约 + yaml 热更；**切面前置检查、记账在 `AiModelSupport`**（切面按上限估**差一个量级**）；**流式拦不住 AOP** → `ChatBudgetGuard.exhausted()` 同判据不重复记账（循环中途降级同调这一处）；`budget.store` 多副本必须 `redis`（内存版日限放大 N 倍）；**scenario 必须与 `AiCallScope.budgetScenario()` 一致否则预算静默失效**（注解只能写字面量，编译期发现不了，由 `TokenBudgetScenarioContractTest` 反射钉住；新增带 `@TokenBudget` 的类要登记进该测试的类清单）
- **Port 方向不反转**（端口 product 定义、ai 实现，ai 不碰 product 表）；**反馈导出只出 `helpful=1 AND scope='chat'`**（**helpful=0 不能自动成金标准**）；供应商可换 = 改 `AiModelConfig` / `easyorange.ai.*`，重试走 openai-java 内置无自研

### admin

- **禁直接依赖他模块 Mapper / DO**：走 `domain/port/Admin*Port`（仪表盘的趋势 / 最近动态同样经端口，admin 不持 `JdbcTemplate`）；写操作记 reason + 操作人 —— reason 落 `eo_user.remark`、操作人落 `audit_info.update_by`，**接口上不许挂不落库的字段**
- 错误码 B6xxx 走 `AdminResultCode` + `AdminDomainException` 具名工厂（`userNotFound` / `orderNotFound` / …），禁裸中文串
- 依赖仅 optional `common` + `framework`，**其余业务模块零依赖**
