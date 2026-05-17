# 测试补充计划

## 背景

后端测试覆盖率存在明显缺口，三个模块（framework/order/admin Controller）合计约 55 个源文件缺少测试。

## 目标

| 模块 | 当前覆盖率 | 目标覆盖率 |
|------|-----------|-----------|
| framework | ~3% | ≥60% (核心) / ≥30% (配置) |
| order | ~11% | ≥50% |
| admin (Controller 层) | 0% | ≥80% |

## 策略：三批并行

```
Batch 1: 纯单元测试 (JUnit 5 + Mockito，无 Spring)
├── framework: 工具类/配置属性/JWT 核心逻辑 (10 类, ~25 方法)
└── order: 值对象/装配器/转换器/状态枚举 (3 类, ~15 方法)

Batch 2: MockMvc Controller (@WebMvcTest + @MockBean)
├── admin: 9 Controller × 42 端点 (~50 方法)
└── order: 2 Controller × 10 端点 (~12 方法)

Batch 3: 集成/仓储/定时任务/事件 (Testcontainers + Mockito)
├── framework: Redis 缓存/Outbox/异常处理 (5 类, ~30 方法)
└── order: 仓储实现/定时任务/事件订阅者 (5 类, ~20 方法)
```

总测试文件数：~35 个，总测试方法数：~150+

---

## Batch 1：纯单元测试

### framework 模块

#### 1. `JwtUtilTest`
- **路径**: `easyorange-framework/src/test/java/.../util/JwtUtilTest.java`
- **依赖**: Mockito (可注入 JwtProperties 或以真实 SecretKey 构造)
- **测试方法**:
  - `generateToken_shouldCreateValidToken()` — 正常生成
  - `generateRefreshToken_shouldCreateValidRefreshToken()` — 刷新 token
  - `parseToken_shouldReturnClaims()` — 解析正常 token
  - `parseToken_withExpiredToken_shouldThrowException()` — 过期 token 抛出异常
  - `parseTokenIgnoreExpiration_shouldReturnClaims()` — 忽略过期解析
  - `validateToken_withValidToken_shouldReturnTrue()` — 有效验证
  - `validateToken_withInvalidSignature_shouldReturnFalse()` — 篡改签名
  - `validateToken_withNullToken_shouldReturnFalse()` — null 输入
  - `isNearExpiration_withNearExpiry_shouldReturnTrue()` — 临近过期检测
  - `isNearExpiration_withFarExpiry_shouldReturnFalse()` — 远未过期
  - `renewTokenIfNeeded_withNearExpiry_shouldRenew()` — 续期
  - `renewTokenIfNeeded_withValidToken_shouldReturnOriginal()` — 不续期
  - `removeBearerPrefix_withBearerPrefix_shouldTrim()` — Bearer 前缀去除
  - `removeBearerPrefix_withoutPrefix_shouldReturnOriginal()` — 无前缀保持
  - `getSubject_shouldReturnSubject()` — 获取主题
  - `extractCustomClaims_shouldReturnCustomClaims()` — 自定义声明

#### 2. `SecurityContextUtilTest`
- **路径**: `easyorange-framework/src/test/java/.../util/SecurityContextUtilTest.java`
- **依赖**: Mockito (mock SecurityContextHolder)
- **测试方法**:
  - `getCurrentUserId_withLongPrincipal_shouldReturnId()` — Long 类型 Principal
  - `getCurrentUserId_withAuthUserPrincipal_shouldReturnId()` — AuthUser 类型
  - `getCurrentUserId_withNullAuthentication_shouldReturnNull()` — 未认证返回 null
  - `getCurrentUserIdOrThrow_withAuth_shouldReturnId()` — 正常
  - `getCurrentUserIdOrThrow_withoutAuth_shouldThrow()` — 未认证抛异常
  - `hasRole_withMatchingRole_shouldReturnTrue()` — 角色匹配
  - `hasRole_withoutRole_shouldReturnFalse()` — 角色不匹配
  - `hasAuthority_withMatchingAuthority_shouldReturnTrue()` — 权限匹配
  - `clearContext_shouldClear()` — 清除上下文
  - `convertPrincipal_withVariousTypes_shouldHandle()` — 多种 Principal 类型
  - `extractRoles_withGrantedAuthorities_shouldExtract()` — 角色提取
  - `buildAuthUser_shouldBuildCorrectly()` — AuthUser 构建

#### 3. `RequestUtilTest`
- **路径**: `easyorange-framework/src/test/java/.../util/RequestUtilTest.java`
- **依赖**: Mockito (mock HttpServletRequest)
- **测试方法**:
  - `getClientIp_withNoProxy_shouldReturnRemoteAddr()` — 直连 IP
  - `getClientIp_withXForwardedFor_shouldReturnFirstIp()` — X-Forwarded-For 第一个 IP
  - `getClientIp_withXRealIP_shouldReturnThatIP()` — X-Real-IP
  - `getClientIp_withMultipleProxies_shouldReturnOriginalClientIP()` — 多级代理
  - `getClientIp_withIPv6Loopback_shouldConvertToIPv4()` — IPv6→IPv4
  - `isValidIp_withValidIPs_shouldReturnTrue()` — 有效 IP
  - `isValidIp_withInvalidIPs_shouldReturnFalse()` — 无效 IP
  - `getFullRequestUrl_shouldBuildFullUrl()` — URL 构建

#### 4. `FileUtilsTest`
- **路径**: `easyorange-framework/src/test/java/.../util/FileUtilsTest.java`
- **依赖**: Mockito (mock MultipartFile)，临时文件系统
- **测试方法**:
  - `assertAllowed_withAllowedExtension_shouldPass()` — 允许扩展名
  - `assertAllowed_withDisallowedExtension_shouldThrow()` — 非法扩展名
  - `assertFileMagicNumber_withValidPng_shouldPass()` — PNG 魔数检测
  - `assertFileMagicNumber_withSpoofedExtension_shouldThrow()` — 伪造扩展名
  - `calculateMd5_shouldReturnCorrectHash()` — MD5 计算
  - `formatFileSize_shouldFormatCorrectly()` — 文件大小格式化 (B/KB/MB/GB)
  - `generateUuidFilename_shouldReturnUuidWithExtension()` — UUID 文件名
  - `getExtension_withVariousFilenames_shouldExtract()` — 扩展名提取
  - `deleteFile_withExistingFile_shouldDelete()` — 文件删除
  - `exists_withExistingFile_shouldReturnTrue()` — 文件存在检测

#### 5. `OperLogUtilTest`
- **路径**: `easyorange-framework/src/test/java/.../util/OperLogUtilTest.java`
- **依赖**: 无
- **测试方法**:
  - `truncate_withShortString_shouldReturnOriginal()` — 短字符串不截断
  - `truncate_withLongString_shouldTruncate()` — 长字符串截断+后缀
  - `truncate_withExactMaxLength_shouldReturnOriginal()` — 正好最大长度
  - `truncate_withNegativeMaxLength_shouldReturnEmpty()` — 负值边界
  - `truncate_withZeroMaxLength_shouldReturnEmpty()` — 零边界
  - `deriveTitle_withControllerSuffix_shouldRemove()` — 去除 Controller 后缀
  - `deriveTitle_withCommandSuffix_shouldRemove()` — 去除 Command 后缀
  - `deriveTitle_withQuerySuffix_shouldRemove()` — 去除 Query 后缀
  - `deriveTitle_withoutSuffix_shouldReturnOriginal()` — 无后缀保持

#### 6. `JwtPropertiesTest`
- **路径**: `easyorange-framework/src/test/java/.../config/properties/JwtPropertiesTest.java`
- **依赖**: 无（直接构造）
- **测试方法**:
  - `validate_withValidProperties_shouldPass()` — 正常
  - `validate_withNullSecretKey_shouldThrow()` — 空密钥
  - `validate_withShortSecretKey_shouldThrow()` — 短密钥 (<32 bytes)
  - `validate_withWeakSecretKey_shouldThrow()` — 弱密钥（全等字符）
  - `defaultValues_shouldBeSet()` — 默认值

#### 7. `SecurityPropertiesTest`
- **路径**: `easyorange-framework/src/test/java/.../config/properties/SecurityPropertiesTest.java`
- **依赖**: 无
- **测试方法**:
  - `validate_withValidProperties_shouldPass()` — 正常
  - `validate_withNullIgnorePaths_shouldThrow()` — null 路径列表
  - `validate_withInvalidPasswordStrength_shouldThrow()` — 密码强度范围外
  - `getIgnorePaths_shouldReturnUnmodifiableList()` — 不可变返回
  - `defaultValues_shouldBeSet()` — 默认值

#### 8. `LoginCacheConstantsTest`
- **路径**: `easyorange-framework/src/test/java/.../constant/LoginCacheConstantsTest.java`
- **依赖**: 无
- **测试方法**:
  - `buildTokenKey_withUserId_shouldReturnCorrectFormat()` — 格式验证
  - `buildAttemptsKey_withUserId_shouldReturnCorrectFormat()` — 格式验证
  - `buildTokenKey_withNullUserId_shouldThrow()` — null 输入

#### 9. `UuidTypeHandlerTest`
- **路径**: `easyorange-framework/src/test/java/.../config/database/UuidTypeHandlerTest.java`
- **依赖**: Mockito (mock PreparedStatement/ResultSet/CallableStatement)
- **测试方法**:
  - `setNonNullParameter_shouldSetUuidString()` — UUID→String
  - `getNullableResult_fromResultSet_shouldReturnUuid()` — ResultSet→UUID
  - `getNullableResult_fromCallableStatement_shouldReturnUuid()` — Callable→UUID
  - `setNonNullParameter_withNullUuid_shouldHandle()` — null 值

#### 10. `OperLogServiceImplTest`
- **路径**: `easyorange-framework/src/test/java/.../operlog/OperLogServiceImplTest.java`
- **依赖**: Mockito (mock SysOperLogMapper)
- **测试方法**:
  - `save_shouldInsertLog()` — 正常保存
  - `save_withNull_shouldThrow()` — null 输入

### order 模块

#### 11. `OrderStatusTest`
- **路径**: `easyorange-order/src/test/java/.../constant/OrderStatusTest.java`
- **依赖**: 无
- **测试方法**:
  - `fromCode_withAllValidCodes_shouldReturnCorrectEnum()` — 6 个合法 code 映射
  - `fromCode_withInvalidCode_shouldThrow()` — 非法 code
  - `getDescByCode_withAllCodes_shouldReturnNonEmpty()` — 所有描述非空
  - `values_shouldCoverAllStates()` — 完整枚举值

#### 12. `OrderVOAssemblerTest`
- **路径**: `easyorange-order/src/test/java/.../assembler/OrderVOAssemblerTest.java`
- **依赖**: Mockito (可选 mock 外部依赖)
- **测试方法**:
  - `toOrderVO_shouldMapAllFields()` — 全字段映射
  - `toOrderVOs_withMultipleOrders_shouldMapAll()` — 多订单转换
  - `toOrderVOs_withEmptyList_shouldReturnEmptyList()` — 空列表
  - `buildProductMap_withProducts_shouldReturnMap()` — 商品映射构建
  - `buildProductMap_withEmptyList_shouldReturnEmptyMap()` — 空商品映射
  - `toOrderVO_withNullFields_shouldHandleGracefully()` — null 字段

#### 13. `OrderDataConverterTest`
- **路径**: `easyorange-order/src/test/java/.../persistence/OrderDataConverterTest.java`
- **依赖**: 无
- **测试方法**:
  - `toDataObject_shouldMapAllFields()` — 聚合→DO 全字段
  - `toAggregate_shouldReconstructFullAggregate()` — DO→聚合重建
  - `toReadModel_shouldMapToReadModel()` — DO→读模型
  - `toDataObject_withMinimalAggregate_shouldHandle()` — 最小聚合
  - `toAggregate_withNullFields_shouldHandleGracefully()` — null 字段
  - `roundtrip_shouldPreserveAllData()` — 往返转换一致性

---

## Batch 2：MockMvc Controller 测试

### 通用测试模式

```java
@WebMvcTest(ControllerClass.class)
@AutoConfigureMockMvc(addFilters = false) // 跳过 JWT 过滤器，改用 @WithMockUser
class ControllerClassTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ServiceClass service;
    
    @Test
    void list_shouldReturn200() throws Exception {
        when(service.method()).thenReturn(...);
        mockMvc.perform(get("/api/..."))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.code").value("A0000"));
    }
    
    @Test
    void list_withInvalidParams_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/...").param("pageNum", "-1"))
               .andExpect(status().isBadRequest());
    }
}
```

### admin 模块

#### 14. `AdminDashboardControllerTest`
- **路径**: `easyorange-admin/src/test/java/.../controller/AdminDashboardControllerTest.java`
- **依赖**: MockBean AdminDashboardService
- **测试方法** (8 端点):
  - `getStats_shouldReturnStats()` — 仪表盘统计
  - `getPendingItems_shouldReturnCounts()` — 待处理计数
  - `getRecentUsers_shouldReturnList()` — 最近用户列表
  - `getRecentProducts_shouldReturnList()` — 最近商品列表
  - `getTrend_shouldReturnTrendData()` — 趋势数据
  - `getActivity_shouldReturnActivity()` — 活动数据
  - `getUserActivityHeatmap_shouldReturnData()` — 热力图
  - `getTopProducts_shouldReturnTopN()` — Top N 商品

#### 15. `AdminUserControllerTest`
- **路径**: `easyorange-admin/src/test/java/.../controller/AdminUserControllerTest.java`
- **依赖**: MockBean AdminUserService
- **测试方法** (3 端点):
  - `listUsers_shouldReturnPage()` — 分页列表
  - `listUsers_withKeyword_shouldFilter()` — 关键字搜索
  - `getUserDetail_shouldReturnUser()` — 用户详情
  - `getUserDetail_withNonExistentId_shouldReturn404()` — 用户不存在
  - `updateUserStatus_shouldSucceed()` — 状态变更
  - `updateUserStatus_withInvalidStatus_shouldReturn400()` — 非法状态

#### 16. `AdminUserControllerExtensionTest`
- **路径**: `easyorange-admin/src/test/java/.../controller/AdminUserControllerExtensionTest.java`
- **依赖**: MockBean AdminUserServiceExtension
- **测试方法** (4 端点):
  - `unlockUser_shouldSucceed()` — 解锁
  - `resetPassword_shouldReturnNewPassword()` — 重置密码
  - `forceLogout_shouldSucceed()` — 强制退出
  - `changeUserRole_shouldSucceed()` — 角色变更
  - 各端点缺少 reason → 400

#### 17. `AdminProductControllerTest`
- **路径**: `easyorange-admin/src/test/java/.../controller/AdminProductControllerTest.java`
- **依赖**: MockBean AdminProductService
- **测试方法** (3 端点):
  - `listProducts_shouldReturnPage()` — 分页列表
  - `listProducts_withCategoryFilter_shouldFilter()` — 分类筛选
  - `getProductDetail_shouldReturnProduct()` — 商品详情
  - `updateProductStatus_shouldSucceed()` — 状态变更
  - `updateProductStatus_withInvalidStatus_shouldReturn400()` — 非法状态

#### 18. `AdminProductAuditControllerTest`
- **路径**: `easyorange-admin/src/test/java/.../controller/AdminProductAuditControllerTest.java`
- **依赖**: MockBean AdminProductAuditService
- **测试方法** (3 端点):
  - `auditProduct_shouldSucceed()` — 单条审核
  - `auditProduct_withInvalidAction_shouldReturn400()` — 非法审核动作
  - `batchAudit_shouldReturnResult()` — 批量审核
  - `batchAudit_withTooManyItems_shouldReturn400()` — 超过 50 条上限
  - `getAuditLogs_shouldReturnList()` — 审核日志
  - `getAuditLogs_withNoLogs_shouldReturnEmptyList()` — 无日志

#### 19. `AdminReportControllerTest`
- **路径**: `easyorange-admin/src/test/java/.../controller/AdminReportControllerTest.java`
- **依赖**: MockBean AdminReportService
- **测试方法** (6 端点):
  - `listReports_shouldReturnPage()` — 分页列表
  - `listReports_withStatusFilter_shouldFilter()` — 状态筛选
  - `getReportDetail_shouldReturnReport()` — 举报详情
  - `getReportHistory_shouldReturnHistory()` — 处理历史
  - `handleReport_withResolveAction_shouldSucceed()` — 处理单条
  - `handleReport_withInvalidAction_shouldReturn400()` — 非法动作
  - `batchHandleReports_shouldSucceed()` — 批量处理
  - `getReportStats_shouldReturnStats()` — 统计数据

#### 20. `AdminOrderControllerTest`
- **路径**: `easyorange-admin/src/test/java/.../controller/AdminOrderControllerTest.java`
- **依赖**: MockBean AdminOrderService
- **测试方法** (6 端点):
  - `listOrders_shouldReturnPage()` — 分页列表
  - `getOrderDetail_shouldReturnDetail()` — 订单详情
  - `getOrderStats_shouldReturnStats()` — 统计
  - `cancelOrder_shouldSucceed()` — 取消订单
  - `forceComplete_shouldSucceed()` — 强制完成
  - `refundOrder_shouldSucceed()` — 退款
  - 各干预端点缺少 reason → 400

#### 21. `AdminCategoryControllerTest`
- **路径**: `easyorange-admin/src/test/java/.../controller/AdminCategoryControllerTest.java`
- **依赖**: MockBean AdminCategoryService
- **测试方法** (6 端点):
  - `listCategories_shouldReturnCategories()` — 分类列表
  - `listCategories_withParentId_shouldFilter()` — 按父 ID 筛选
  - `categoryTree_shouldReturnTree()` — 树形结构
  - `createCategory_shouldReturnCreated()` — 创建分类
  - `createCategory_withInvalidName_shouldReturn400()` — 名称校验
  - `updateCategory_shouldSucceed()` — 更新分类
  - `updateStatus_shouldSucceed()` — 状态变更
  - `deleteCategory_shouldSucceed()` — 删除分类

#### 22. `AdminReviewControllerTest`
- **路径**: `easyorange-admin/src/test/java/.../controller/AdminReviewControllerTest.java`
- **依赖**: MockBean AdminReviewService
- **测试方法** (3 端点):
  - `listReviews_shouldReturnPage()` — 分页列表
  - `listReviews_withRatingFilter_shouldFilter()` — 评分筛选
  - `getReviewDetail_shouldReturnReview()` — 评价详情
  - `deleteReview_shouldSucceed()` — 删除评价
  - `deleteReview_withoutReason_shouldReturn400()` — 缺少原因

### order 模块

#### 23. `OrderCommandControllerTest`
- **路径**: `easyorange-order/src/test/java/.../controller/OrderCommandControllerTest.java`
- **依赖**: MockBean OrderCommandHandler
- **测试方法** (6 端点):
  - `createOrder_shouldReturnResult()` — 创建订单
  - `createOrder_withInvalidPhone_shouldReturn400()` — 手机号校验
  - `cancelOrder_shouldSucceed()` — 取消订单
  - `payOrder_shouldSucceed()` — 支付订单
  - `shipOrder_shouldSucceed()` — 发货
  - `receiveOrder_shouldSucceed()` — 确认收货
  - `refundOrder_shouldSucceed()` — 退款
  - 各操作订单不存在 → 404

#### 24. `OrderQueryControllerTest`
- **路径**: `easyorange-order/src/test/java/.../controller/OrderQueryControllerTest.java`
- **依赖**: MockBean OrderQueryHandler
- **测试方法** (4 端点):
  - `getOrderDetail_shouldReturnOrder()` — 订单详情
  - `getOrderDetail_withNonExistentId_shouldReturn404()` — 不存在
  - `getMyOrders_shouldReturnPage()` — 我的订单
  - `getMyOrders_withEmpty_shouldReturnEmptyPage()` — 空列表
  - `getSoldOrders_shouldReturnPage()` — 卖出订单
  - `getOrderList_shouldReturnPage()` — 通用列表

---

## Batch 3：集成/仓储/定时任务/事件测试

### framework 模块

#### 25. `RedisCacheImplIntegrationTest`
- **路径**: `easyorange-framework/src/test/java/.../redis/RedisCacheImplIntegrationTest.java`
- **依赖**: Testcontainers Redis (`GenericContainer`)
- **测试方法**:
  - **字符串**: set/get/setIfAbsent/delete/hasKey → 基本 CRUD
  - **过期**: expire/ttl → TTL 设置和读取
  - **原子操作**: increment/decrement → 计数
  - **批量**: multiGet/multiSet → 批量读写
  - **Hash**: hashPut/hashGet/hashDelete/hasHashKey → Hash 全操作
  - **List**: listPush/listPop/listRange → 列表操作
  - **Set**: setAdd/setMembers/setIsMember → 集合操作
  - **ZSet**: zsetAdd/zsetRangeByScore → 有序集合
  - **Lua**: executeLuaScript → 脚本执行
  - **锁**: tryLock/unlock → 分布式锁
  - **锁竞争**: 并发锁互斥 → 不可重入
  - **类型安全**: get(Class) with wrong type → CacheTypeMismatchException
  - **键前缀**: generateKey/stripPrefix → 前缀正确性
  - **null 安全**: 各类 null 输入 → 抛出或返回默认

#### 26. `EventIdempotencyCheckerIntegrationTest`
- **路径**: `easyorange-framework/src/test/java/.../event/EventIdempotencyCheckerIntegrationTest.java`
- **依赖**: Testcontainers Redis
- **测试方法**:
  - `isDuplicate_withNewEvent_shouldReturnFalse()` — 新事件非重复
  - `isDuplicate_withDuplicateEvent_shouldReturnTrue()` — 重复事件
  - `tryMark_shouldSetKey()` — 标记处理中
  - `markProcessed_shouldPersist()` — 标记已完成
  - `remove_shouldDeleteKey()` — 移除标记
  - `isDuplicate_afterMarkProcessed_shouldReturnTrue()` — 处理完仍检测重复
  - 幂等键格式正确性

#### 27. `OutboxRepositoryIntegrationTest`
- **路径**: `easyorange-framework/src/test/java/.../outbox/OutboxRepositoryIntegrationTest.java`
- **依赖**: Testcontainers MySQL + Flyway
- **测试方法**:
  - `save_shouldInsertRecord()` — 保存 outbox 消息
  - `findPending_shouldReturnPendingOnly()` — 仅查询待处理
  - `findPending_withLimit_shouldRespectMax()` — 分页限制
  - `markAsPublished_shouldUpdateStatus()` — 标记已发布
  - `markAsFailed_shouldUpdateStatusAndError()` — 标记失败+错误信息
  - `save_and_findPending_roundtrip()` — 完整生命周期
  - `findPending_withEmptyTable_shouldReturnEmpty()` — 空表

#### 28. `GlobalExceptionHandlerTest`
- **路径**: `easyorange-framework/src/test/java/.../exception/GlobalExceptionHandlerTest.java`
- **依赖**: MockMvc (standalone setup)
- **测试方法** (14 种异常):
  - `handleBusinessException_shouldReturn400()` — BaseBusinessException
  - `handleParamValidation_shouldReturn400()` — ParamValidationException
  - `handleFileException_shouldReturn400()` — FileException
  - `handleAccessDenied_shouldReturn403()` — AccessDeniedException
  - `handleMethodArgumentNotValid_shouldReturn400()` — @Valid 校验失败
  - `handleBindException_shouldReturn400()` — 绑定异常
  - `handleConstraintViolation_shouldReturn400()` — 参数约束
  - `handleDuplicateKey_shouldReturn400()` — 唯一键冲突
  - `handleMissingParam_shouldReturn400()` — 缺少参数
  - `handleTypeMismatch_shouldReturn400()` — 类型转换
  - `handleNoHandler_shouldReturn404()` — 404
  - `handleMethodNotAllowed_shouldReturn405()` — 方法不支持
  - `handleGenericException_shouldReturn500()` — 500 兜底
  - 验证全部返回 Result 格式 code/message/data

### order 模块

#### 29. `MybatisOrderRepositoryIntegrationTest`
- **路径**: `easyorange-order/src/test/java/.../persistence/MybatisOrderRepositoryIntegrationTest.java`
- **依赖**: Testcontainers MySQL + Flyway
- **测试方法**:
  - `save_shouldInsertOrder()` — 新增订单
  - `update_shouldUpdateFields()` — 更新订单
  - `findById_shouldReturnOrder()` — ID 查询
  - `findById_withNonExistentId_shouldReturnEmpty()` — 不存在
  - `findByBuyerId_shouldReturnOrders()` — 买家查询
  - `findBySellerId_shouldReturnOrders()` — 卖家查询
  - `findExpiredOrders_shouldReturnExpiredOnly()` — 过期订单
  - `findByStatus_shouldFilterByStatus()` — 状态筛选
  - `findShippedOrdersBefore_shouldReturnOldShipped()` — 发货超时

#### 30. `MybatisOrderReadRepositoryIntegrationTest`
- **路径**: `easyorange-order/src/test/java/.../persistence/MybatisOrderReadRepositoryIntegrationTest.java`
- **依赖**: Testcontainers MySQL + Flyway
- **测试方法**:
  - `findById_shouldReturnReadModel()` — 读模型详情
  - `findPage_shouldReturnPagedResults()` — 分页查询
  - `findPage_withCondition_shouldFilter()` — 条件筛选
  - `findPage_withEmptyResult_shouldReturnEmptyPage()` — 空结果
  - `countByStatus_shouldReturnCorrectCount()` — 状态计数

#### 31. `OrderTimeoutTaskTest`
- **路径**: `easyorange-order/src/test/java/.../job/OrderTimeoutTaskTest.java`
- **依赖**: Mockito (mock OrderRepository, RedisCache)
- **测试方法**:
  - `cancelExpiredOrders_shouldCancel()` — 正常超时取消
  - `cancelExpiredOrders_withLockFailure_shouldSkip()` — 锁竞争跳过
  - `cancelExpiredOrders_withNoExpiredOrders_shouldDoNothing()` — 无超时订单
  - `cancelExpiredOrders_withPartialFailure_shouldContinue()` — 部分失败继续

#### 32. `OrderAutoConfirmTaskTest`
- **路径**: `easyorange-order/src/test/java/.../job/OrderAutoConfirmTaskTest.java`
- **依赖**: Mockito (mock OrderRepository 等)
- **测试方法**:
  - `autoConfirmReceipt_shouldConfirm()` — 正常自动确认
  - `autoConfirmReceipt_withNoShippedOrders_shouldDoNothing()` — 无待确认
  - `autoConfirmReceipt_withException_shouldHandle()` — 异常处理

#### 33. `OrderEventSubscribersTest`
- **路径**: `easyorange-order/src/test/java/.../mq/OrderEventSubscribersTest.java`
- **依赖**: Mockito
- **测试方法**:
  - `onOrderCreated_shouldPublishStockReservation()` — 创建→保留库存
  - `onOrderCancelled_shouldRestoreStock()` — 取消→恢复库存
  - `onOrderCompleted_shouldMarkAsSold()` — 完成→标记已售
  - `onOrderRefunded_shouldRestoreStock()` — 退款→恢复库存
  - `onOrderCancel_withNonExistentOrder_shouldHandle()` — 异常处理

#### 34. `OrderNotificationEventSubscriberTest`
- **路径**: `easyorange-order/src/test/java/.../mq/OrderNotificationEventSubscriberTest.java`
- **依赖**: Mockito (mock NotificationService, EventIdempotencyChecker)
- **测试方法**:
  - `onOrderCreated_shouldSendNotification()` — 创建通知
  - `onOrderPaid_shouldSendNotification()` — 支付通知
  - `onOrderShipped_shouldSendNotification()` — 发货通知
  - `onOrderCompleted_shouldSendNotification()` — 完成通知
  - `onOrderCancelled_shouldSendNotification()` — 取消通知
  - `onOrderRefunded_shouldSendNotification()` — 退款通知
  - `onOrderCreated_withDuplicateEvent_shouldSkip()` — 幂等跳过
  - `onOrderCreated_withUnknownUser_shouldHandle()` — 未知用户

---

## 测试标准

| 维度 | 标准 |
|------|------|
| 命名 | `{被测类}Test`，嵌套类 `{被测方法}Tests` |
| 结构 | AAA 模式（Arrange → Act → Assert），空行分隔 |
| 断言 | AssertJ 流畅断言：`assertThat`/`assertThatThrownBy` |
| MockMvc | `jsonPath` + `status()` 断言 |
| Mockito | `@Mock` + `@InjectMocks` / `@MockBean` |
| 无 Spring 上下文 | Batch 1 全部纯单元 |
| 隔离 | 每个测试方法独立，`@BeforeEach` 重置 |
| 集成测试 | Testcontainers + `@Tag("integration")` |
| 覆盖目标 | 单元 >90%，Controller >80%，集成 >70% |
