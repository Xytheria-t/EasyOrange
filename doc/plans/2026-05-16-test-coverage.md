# 测试补充实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 framework 模块测试覆盖率从 ~3% 提升至 ≥60%，order 从 ~11% 提升至 ≥50%，admin Controller 从 0% 提升至 ≥80%

**Architecture:** 三批独立并行——Batch 1 纯单元测试（JUnit 5 + Mockito 无 Spring 上下文）、Batch 2 MockMvc Controller 测试（@WebMvcTest + @MockBean）、Batch 3 集成/仓储/定时任务测试（Testcontainers + Mockito）

**Tech Stack:** JUnit 5, Mockito, AssertJ, Spring MockMvc, Testcontainers

---

## 测试模式参考

### Batch 1 模式：纯单元测试

```java
// framework 工具类测试模板
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {
    @Mock
    private JwtProperties jwtProperties;
    @InjectMocks
    private JwtUtil jwtUtil;
    
    @BeforeEach
    void setUp() {
        // 通用初始化
    }
    
    @Test
    void methodName_shouldExpectedBehavior() {
        // Arrange
        when(jwtProperties.getSecretKey()).thenReturn("base64-encoded-256-bit-key...");
        // Act
        String token = jwtUtil.generateToken(1L);
        // Assert
        assertThat(token).isNotBlank();
        assertThat(jwtUtil.parseToken(token)).isNotNull();
    }
}
```

**纯 POJO 测试**（无 Mockito）：
```java
class OperLogUtilTest {
    @Test
    void truncate_withShortString_shouldReturnOriginal() {
        assertThat(OperLogUtil.truncate("hello", 10)).isEqualTo("hello");
    }
}
```

### Batch 2 模式：MockMvc Controller 测试

```java
@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc(addFilters = false) // 跳过 JWT 过滤器
class AdminReportControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AdminReportService adminReportService;
    
    @Test
    void listReports_shouldReturnPage() throws Exception {
        when(adminReportService.listReports(any(), any(), any()))
            .thenReturn(PageResult.success(Collections.emptyList(), 1, 20, 0));
        
        mockMvc.perform(get("/api/admin/reports"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.code").value("A0000"))
               .andExpect(jsonPath("$.data.list").isArray());
    }
}
```

### Batch 3 模式：Testcontainers 集成测试

```java
@SpringBootTest
@Tag("integration")
@Testcontainers
class RedisCacheImplIntegrationTest {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
    
    @Autowired
    private RedisCacheImpl redisCache;
    
    @BeforeEach
    void setUp() {
        redisCache.delete("*");
    }
    
    @Test
    void setAndGet_shouldWork() {
        redisCache.set("key", "value");
        assertThat(redisCache.get("key")).isEqualTo("value");
    }
}
```

---

## Batch 1：单元测试（framework + order）

### Batch 1.1: framework 工具类（8 个测试文件）

**Task 1.1.1: OperLogUtilTest**
- **文件**: `easyorange-framework/src/test/java/com/cartethyia/easyorange/framework/util/OperLogUtilTest.java`
- **依赖**: 无（纯静态方法）
- [ ] **创建测试文件**

```java
package com.cartethyia.easyorange.framework.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OperLogUtilTest {

    // === truncate ===
    @Test
    void truncate_withShortString_shouldReturnOriginal() {
        assertThat(OperLogUtil.truncate("hello", 10)).isEqualTo("hello");
    }

    @Test
    void truncate_withLongString_shouldTruncate() {
        assertThat(OperLogUtil.truncate("hello world", 8)).isEqualTo("hello...");
    }

    @Test
    void truncate_withExactMaxLength_shouldReturnOriginal() {
        assertThat(OperLogUtil.truncate("12345678", 8)).isEqualTo("12345678");
    }

    @Test
    void truncate_withNegativeMaxLength_shouldReturnEmpty() {
        assertThat(OperLogUtil.truncate("hello", -1)).isEmpty();
    }

    @Test
    void truncate_withZeroMaxLength_shouldReturnEmpty() {
        assertThat(OperLogUtil.truncate("hello", 0)).isEmpty();
    }

    @Test
    void truncate_withNullInput_shouldReturnEmpty() {
        assertThat(OperLogUtil.truncate(null, 10)).isEmpty();
    }

    // === deriveTitle ===
    @Test
    void deriveTitle_withControllerSuffix_shouldRemove() {
        assertThat(OperLogUtil.deriveTitle("UserController")).isEqualTo("User");
    }

    @Test
    void deriveTitle_withCommandSuffix_shouldRemove() {
        assertThat(OperLogUtil.deriveTitle("CreateUserCommand")).isEqualTo("CreateUser");
    }

    @Test
    void deriveTitle_withQuerySuffix_shouldRemove() {
        assertThat(OperLogUtil.deriveTitle("GetUserQuery")).isEqualTo("GetUser");
    }

    @Test
    void deriveTitle_withoutSuffix_shouldReturnOriginal() {
        assertThat(OperLogUtil.deriveTitle("UserService")).isEqualTo("UserService");
    }

    @Test
    void deriveTitle_withNullInput_shouldReturnNull() {
        assertThat(OperLogUtil.deriveTitle(null)).isNull();
    }
}
```

- [ ] **运行测试确认通过**
  ```bash
  cd easyorange-backend
  ./mvnw test -pl easyorange-framework -Dtest=OperLogUtilTest -DfailIfNoTests=false
  ```

- [ ] **Commit**
  ```bash
  git add easyorange-backend/easyorange-framework/src/test/java/com/cartethyia/easyorange/framework/util/OperLogUtilTest.java
  git commit -m "test(framework): add OperLogUtil unit tests"
  ```

---

**Task 1.1.2: LoginCacheConstantsTest**
- **文件**: `easyorange-framework/src/test/java/com/cartethyia/easyorange/framework/constant/LoginCacheConstantsTest.java`
- **测试方法**:
  - `buildTokenKey_withUserId_shouldReturnCorrectFormat()`
  - `buildAttemptsKey_withUserId_shouldReturnCorrectFormat()`
  - `buildTokenKey_withNullUserId_shouldThrow()`
- [ ] **创建测试文件（follow LoginCacheConstants API）**
- [ ] **运行测试确认通过**
- [ ] **Commit**

---

**Task 1.1.3: CacheTypeMismatchExceptionTest**
- **文件**: `easyorange-framework/src/test/java/com/cartethyia/easyorange/framework/exception/CacheTypeMismatchExceptionTest.java`
- **测试方法**:
  - `constructor_shouldSetMessage()`
  - `getExpectedType_shouldReturnExpectedType()`
  - `getActualType_shouldReturnActualType()`
  - `getKey_shouldReturnKey()`
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

---

**Task 1.1.4: JwtUtilTest**
- **文件**: `easyorange-framework/src/test/java/com/cartethyia/easyorange/framework/util/JwtUtilTest.java`
- **依赖**: JwtProperties + SecretKey（可使用真实密钥构造）
- **测试方法**:
  - `generateToken_shouldCreateValidToken()` / `generateRefreshToken_shouldCreateValidRefreshToken()` — 生成验证
  - `parseToken_shouldReturnClaims()` / `parseToken_withExpiredToken_shouldThrowException()` — 解析&过期
  - `validateToken_withValidToken_shouldReturnTrue()` / `validateToken_withInvalidSignature_shouldReturnFalse()` — 有效&篡改
  - `isNearExpiration_withNearExpiry_shouldReturnTrue()` / `isNearExpiration_withFarExpiry_shouldReturnFalse()` — 过期临近
  - `renewTokenIfNeeded_withNearExpiry_shouldRenew()` / `renewTokenIfNeeded_withValidToken_shouldReturnOriginal()` — 续期
  - `removeBearerPrefix_withBearerPrefix_shouldTrim()` / `removeBearerPrefix_withoutPrefix_shouldReturnOriginal()` — Bearer 处理
  - `getSubject_shouldReturnSubject()` / `extractCustomClaims_shouldReturnCustomClaims()` — 声明提取
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

---

**Task 1.1.5: SecurityContextUtilTest**
- **文件**: `easyorange-framework/src/test/java/com/cartethyia/easyorange/framework/util/SecurityContextUtilTest.java`
- **依赖**: Mockito (mock SecurityContextHolder / Authentication)
- **测试方法**:
  - `getCurrentUserId_withLongPrincipal_shouldReturnId()` — Long Principal
  - `getCurrentUserId_withAuthUserPrincipal_shouldReturnId()` — AuthUser Principal
  - `getCurrentUserId_withNullAuthentication_shouldReturnNull()` — 未认证
  - `getCurrentUserIdOrThrow_withAuth_shouldReturnId()` / `getCurrentUserIdOrThrow_withoutAuth_shouldThrow()` — 带/不带异常
  - `hasRole_withMatchingRole_shouldReturnTrue()` / `hasRole_withoutRole_shouldReturnFalse()` — 角色
  - `hasAuthority_withMatchingAuthority_shouldReturnTrue()` — 权限
  - `clearContext_shouldClear()` — 清除
  - `buildAuthUser_shouldBuildCorrectly()` — AuthUser 构建
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

---

**Task 1.1.6: RequestUtilTest**
- **文件**: `easyorange-framework/src/test/java/com/cartethyia/easyorange/framework/util/RequestUtilTest.java`
- **依赖**: Mockito (mock HttpServletRequest)
- **测试方法**:
  - `getClientIp_withNoProxy_shouldReturnRemoteAddr()` — 直连
  - `getClientIp_withXForwardedFor_shouldReturnFirstIp()` — 代理头
  - `getClientIp_withXRealIP_shouldReturnThatIP()` — X-Real-IP
  - `getClientIp_withMultipleProxies_shouldReturnOriginalClientIP()` — 多级代理
  - `getClientIp_withIPv6Loopback_shouldConvertToIPv4()` — IPv6→IPv4
  - `getFullRequestUrl_shouldBuildFullUrl()` — URL 构建
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

---

**Task 1.1.7: FileUtilsTest**
- **文件**: `easyorange-framework/src/test/java/com/cartethyia/easyorange/framework/util/FileUtilsTest.java`
- **依赖**: Mockito (mock MultipartFile), 临时文件系统
- **测试方法**:
  - 扩展名校验、魔数检测、MD5 计算、文件大小格式化、UUID 文件名、路径穿越防护
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

---

**Task 1.1.8: JwtPropertiesTest / SecurityPropertiesTest**
- **文件**: 
  - `easyorange-framework/src/test/java/.../config/properties/JwtPropertiesTest.java`
  - `easyorange-framework/src/test/java/.../config/properties/SecurityPropertiesTest.java`
- **测试方法**:
  - JwtProperties: `validate()` 空密钥/短密钥/弱密钥/正常
  - SecurityProperties: `validate()` null 路径/密码强度/不可变列表
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

---

### Batch 1.2: order 模块单元测试（3 个测试文件）

**Task 1.2.1: OrderStatusTest**
- **文件**: `easyorange-order/src/test/java/com/cartethyia/easyorange/order/domain/constant/OrderStatusTest.java`
- **测试方法**: `fromCode` 6 个状态映射 + 非法 code + `getDescByCode` 非空
- [ ] **创建测试文件**
- [ ] **运行测试** `./mvnw test -pl easyorange-order -Dtest=OrderStatusTest`
- [ ] **Commit**

---

**Task 1.2.2: OrderVOAssemblerTest**
- **文件**: `easyorange-order/src/test/java/.../application/assembler/OrderVOAssemblerTest.java`
- **测试方法**: 全字段映射、多订单、空列表、null 字段
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

---

**Task 1.2.3: OrderDataConverterTest**
- **文件**: `easyorange-order/src/test/java/.../persistence/OrderDataConverterTest.java`
- **测试方法**: toDataObject/toAggregate/toReadModel + 空字段 + 往返一致性
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

---

## Batch 2：Controller MockMvc 测试

### Batch 2.1: admin Controller（9 个测试文件）

**模式**：`@WebMvcTest(Controller.class)` + `@AutoConfigureMockMvc(addFilters = false)` + `@MockBean Service`

**Task 2.1.1: AdminDashboardControllerTest**
- **文件**: `easyorange-admin/src/test/java/.../controller/AdminDashboardControllerTest.java`
- **端点**: 8 个 GET → mock service 返回，验证 JSON path
- [ ] **创建测试文件**
- [ ] **运行测试** `./mvnw test -pl easyorange-admin -Dtest=AdminDashboardControllerTest`
- [ ] **Commit**

**Task 2.1.2: AdminUserControllerTest**
- **文件**: `easyorange-admin/src/test/java/.../controller/AdminUserControllerTest.java`
- **端点**: 3 个（list/detail/status）+ 校验
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 2.1.3: AdminUserControllerExtensionTest**
- **文件**: `easyorange-admin/src/test/java/.../controller/AdminUserControllerExtensionTest.java`
- **端点**: 4 个（unlock/resetPassword/forceLogout/changeRole）
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 2.1.4: AdminProductControllerTest**
- **文件**: `easyorange-admin/src/test/java/.../controller/AdminProductControllerTest.java`
- **端点**: 3 个（list/detail/status）
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 2.1.5: AdminProductAuditControllerTest**
- **文件**: `easyorange-admin/src/test/java/.../controller/AdminProductAuditControllerTest.java`
- **端点**: 3 个（audit/batch-audit/audit-logs）
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 2.1.6: AdminReportControllerTest**
- **文件**: `easyorange-admin/src/test/java/.../controller/AdminReportControllerTest.java`
- **端点**: 6 个（list/detail/history/handle/batch-handle/stats）
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 2.1.7: AdminOrderControllerTest**
- **文件**: `easyorange-admin/src/test/java/.../controller/AdminOrderControllerTest.java`
- **端点**: 6 个（list/detail/stats/cancel/force-complete/refund）
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 2.1.8: AdminCategoryControllerTest**
- **文件**: `easyorange-admin/src/test/java/.../controller/AdminCategoryControllerTest.java`
- **端点**: 6 个（list/tree/create/update/status/delete）
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 2.1.9: AdminReviewControllerTest**
- **文件**: `easyorange-admin/src/test/java/.../controller/AdminReviewControllerTest.java`
- **端点**: 3 个（list/detail/delete）
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

### Batch 2.2: order Controller（2 个测试文件）

**Task 2.2.1: OrderCommandControllerTest**
- **文件**: `easyorange-order/src/test/java/.../controller/OrderCommandControllerTest.java`
- **端点**: 6 个（create/cancel/pay/ship/receive/refund）
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 2.2.2: OrderQueryControllerTest**
- **文件**: `easyorange-order/src/test/java/.../controller/OrderQueryControllerTest.java`
- **端点**: 4 个（detail/my/sold/list）
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

---

## Batch 3：集成/仓储/定时任务/事件测试

### Batch 3.1: framework 集成测试（4 个测试文件）

**Task 3.1.1: RedisCacheImplIntegrationTest**
- **文件**: `easyorange-framework/src/test/java/.../redis/RedisCacheImplIntegrationTest.java`
- **依赖**: Testcontainers Redis
- **覆盖**: String CRUD、过期、原子操作、批量、Hash/List/Set/ZSet、Lua、分布式锁、类型异常、null 安全
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 3.1.2: EventIdempotencyCheckerIntegrationTest**
- **文件**: `easyorange-framework/src/test/java/.../event/EventIdempotencyCheckerIntegrationTest.java`
- **依赖**: Testcontainers Redis
- **覆盖**: 新事件非重复、重复事件、tryMark/markProcessed/remove、幂等键格式
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 3.1.3: OutboxRepositoryIntegrationTest**
- **文件**: `easyorange-framework/src/test/java/.../outbox/OutboxRepositoryIntegrationTest.java`
- **依赖**: Testcontainers MySQL + Flyway
- **覆盖**: save/findPending/markAsPublished/markAsFailed + 空表 + 分页
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 3.1.4: GlobalExceptionHandlerTest**
- **文件**: `easyorange-framework/src/test/java/.../exception/GlobalExceptionHandlerTest.java`
- **依赖**: MockMvc standalone setup
- **覆盖**: 14 种异常处理器 → 验证状态码 + Result 格式
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

### Batch 3.2: order 集成/定时任务/事件测试（6 个测试文件）

**Task 3.2.1: MybatisOrderRepositoryIntegrationTest**
- **文件**: `easyorange-order/src/test/java/.../persistence/MybatisOrderRepositoryIntegrationTest.java`
- **依赖**: Testcontainers MySQL + Flyway
- **覆盖**: 8 个 Repository 方法
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 3.2.2: MybatisOrderReadRepositoryIntegrationTest**
- **文件**: `easyorange-order/src/test/java/.../persistence/MybatisOrderReadRepositoryIntegrationTest.java`
- **依赖**: Testcontainers MySQL + Flyway
- **覆盖**: findPage/findById/countByStatus
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 3.2.3: OrderTimeoutTaskTest**
- **文件**: `easyorange-order/src/test/java/.../job/OrderTimeoutTaskTest.java`
- **依赖**: Mockito
- **覆盖**: 正常取消、锁竞争、无过期、部分失败
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 3.2.4: OrderAutoConfirmTaskTest**
- **文件**: `easyorange-order/src/test/java/.../job/OrderAutoConfirmTaskTest.java`
- **依赖**: Mockito
- **覆盖**: 正常确认、无待确认、异常处理
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 3.2.5: OrderEventSubscribersTest**
- **文件**: `easyorange-order/src/test/java/.../mq/OrderEventSubscribersTest.java`
- **依赖**: Mockito
- **覆盖**: 4 个 event subscriber（stock reservation/restore/markAsSold）
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

**Task 3.2.6: OrderNotificationEventSubscriberTest**
- **文件**: `easyorange-order/src/test/java/.../mq/OrderNotificationEventSubscriberTest.java`
- **依赖**: Mockito (mock NotificationService + EventIdempotencyChecker)
- **覆盖**: 6 种订单事件通知 + 幂等跳过 + 未知用户
- [ ] **创建测试文件**
- [ ] **运行测试确认通过**
- [ ] **Commit**

---

## 实施策略

1. **Batch 1 和 Batch 2 完全独立**，可以并行推进
2. **Batch 3** 需要 Testcontainers（MySQL + Redis 容器），依赖 Docker 环境
3. 建议执行顺序：
   - 优先推进 **Batch 1**（最快，纯单元无环境依赖）
   - 并行推进 **Batch 2**（MockMvc 切 Controller 层）
   - Batch 3 在 Docker 准备就绪后启动

## 验证

所有测试编写完成后，全局验证：
```bash
cd easyorange-backend && ./mvnw test -DexcludedGroups=integration
```
