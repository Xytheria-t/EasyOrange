# 命名规范审计报告

> 生成日期: 2026-05-22
> 范围: `easyorange-backend/` (Java) + `easyorange-frontend/` (TypeScript/React)

---

## 一、后端命名问题 (Backend)

### 🔴 高优先级

#### H1. 同一模块两处 `MessageType` 枚举（概念歧义）

| 文件 | 当前名称 | 含义 | 建议改名 |
|------|---------|------|---------|
| `message/enums/MessageType.java` | `MessageType` | 消息类别: SYSTEM/CHAT/ORDER/PAYMENT/ACTIVITY | 保留 |
| `message/domain/valueobject/MessageType.java` | `MessageType` | 消息内容格式: TEXT/MARKDOWN | `MessageContentFormat` |

**问题**: 同一模块 (`easyorange-message`) 中存在两个 `MessageType` 枚举，代表完全不
同的概念。`enums/MessageType` 是消息分类（系统/聊天/订单），
`valueobject/MessageType` 是消息内容格式（纯文本/Markdown）。开发者混淆风险高。

**建议**: 将 `domain/valueobject/MessageType.java` → `MessageContentFormat.java`

---

#### H2. `IResultCode` 接口的匈牙利 I-前缀

| 文件 | 当前名称 | 建议 |
|------|---------|------|
| `common/enums/IResultCode.java` | `IResultCode` | `ResultCode` 或保留 |

**问题**: 现代 Java 已不推荐接口使用 `I` 前缀（`List`, `Map`, `Service` 等均无前
缀）。`ResultCode` 枚举实现了该接口。

**影响**: 全局使用（被 `Result.java`, `BaseBusinessException.java` 等广泛引用），
改名需全项目重构。

**建议**: 短期保留别名兼容，长期逐步迁移。

---

#### H3. `Extension` 后缀类命名模糊

| 文件 | 当前名称 | 建议 |
|------|---------|------|
| `admin/service/AdminUserServiceExtension.java` | `AdminUserServiceExtension` | `AdminUserSecurityService` |
| `admin/controller/AdminUserControllerExtension.java` | `AdminUserControllerExtension` | 合并或改为 `AdminUserSecurityController` |

**问题**: `Extension` 含义模糊，实为密码重置/解锁/角色管理等安全操作。在 Java 后
端中通常不使用此后缀。

---

### 🟡 中优先级

#### M1. 缩写方法名 `delToken`

| 文件 | 当前名称 | 建议 |
|------|---------|------|
| `framework/service/TokenService.java` | `delToken(String token)` | `deleteToken(String token)` |
| `framework/service/impl/TokenServiceImpl.java` | `delToken(String token)` | `deleteToken(String token)` |

**问题**: `del` 是 delete 的缩写，不符合全名约定。

---

#### M2. `domain/constants/` vs `domain/constant/` 包名不一致

| 模块 | 包路径 | 模式 |
|------|--------|------|
| `user` | `domain/constants/UserConstant.java` | 复数 `constants` ❌ |
| `user` | `domain/constants/UserSecurityConstant.java` | 复数 `constants` ❌ |
| `product` | `domain/constant/ProductConstant.java` | 单数 `constant` ✅ |
| `order` | `domain/constant/OrderConstant.java` | 单数 `constant` ✅ |
| `payment` | `domain/constant/PaymentStatus.java` | 单数 `constant` ✅ |
| `message` | `constant/MessageConstant.java` | 单数 `constant` ✅ |

**问题**: `user` 模块使用复数 `constants`，其他模块均用单数 `constant`。

**建议**: 将 `user/domain/constants/` → `user/domain/constant/`

---

#### M3. 测试方法命名过于简短

以下测试方法仅通过 `@DisplayName` 描述含义，方法名本身过于泛化：

| 文件 | 方法名 | 建议 |
|------|--------|------|
| `user/.../AuthenticationServiceTest.java` | `success()` (重复3次) | `authenticate_withValidCredentials_shouldSucceed()` |
| `user/.../AuthenticationServiceTest.java` | `userNotFound()` (重复3次) | `authenticate_withNonExistentUser_shouldFail()` |
| `user/.../AuthenticationServiceTest.java` | `wrongPassword()` | `authenticate_withWrongPassword_shouldFail()` |
| `user/.../AuthenticationServiceTest.java` | `userDisabled()` | `authenticate_withDisabledUser_shouldFail()` |
| `user/.../LoginSecurityServiceTest.java` | `success()` | `validateLogin_withValidAttempt_shouldSucceed()` |
| `user/.../SmsCodeServiceTest.java` | `success()` | `sendSmsCode_withValidPhone_shouldSucceed()` |
| `framework/.../RedisCacheImplIntegrationTest.java` | `expire()` | `shouldExpireKeyWhenTtlReached()` |

---

### 🟢 低优先级

#### L1. 测试变量缩写 `convWithUser2/User3`

| 文件 | 当前名称 | 建议 |
|------|---------|------|
| `message/.../ConversationQueryHandlerTest.java` | `convWithUser2`, `convWithUser3` | `conversationWithUser2`, `conversationWithUser3` |

---

## 二、前端命名问题 (Frontend)

### 🔴 高优先级

#### FH1. 单字母变量（生产代码）

| 文件 | 行号 | 变量 | 建议 |
|------|------|------|------|
| `src/utils/format.ts` | 35, 54 | `d` (Date对象) | `dateObj` |
| `src/components/ai/AiQaPanel.tsx` | 24-25 | `h`, `m` (时分) | `hours`, `minutes` |
| `src/admin/pages/orders/OrderDetailModal.tsx` | 67 | `o` (订单数据) | `orderData` |
| `src/admin/pages/products/ProductManageDetailModal.tsx` | 104 | `p` (商品数据) | `productData` |
| `src/admin/pages/reviews/ReviewManagePage.tsx` | 104 | `t` (评分标签) | `ratingLabel` |
| `src/admin/pages/reports/ReportManagePage.tsx` | 90 | `t` (类型标签) | `typeLabel` |
| `src/admin/pages/dashboard/charts/TopProductsChart.tsx` | 57 | `p` (payload) | `payload` |

#### FH2. API 方法名过于泛化（缺乏上下文）

`favoriteApi.ts`:

| 当前名 | 建议名 |
|--------|--------|
| `getList(params)` | `getFavorites(params)` |
| `add(productId)` | `addFavorite(productId)` |
| `remove(productId)` | `removeFavorite(productId)` |
| `check(productId)` | `checkFavorite(productId)` |

`orderApi.ts`:

| 当前名 | 建议名 |
|--------|--------|
| `getDetail(id)` | `getOrderDetail(id)` |
| `cancel(id)` | `cancelOrder(id)` |
| `receive(id)` | `receiveOrder(id)` |
| `pay(id)` | `payOrder(id)` |
| `ship(id)` | `shipOrder(id)` |
| `refund(id)` | `refundOrder(id)` |

`messageApi.ts`:

| 当前名 | 建议名 |
|--------|--------|
| `getList(_params)` | `getMessages(params)` |
| `delete(id)` | `deleteMessage(id)` |
| `recall(messageId)` | `recallMessage(messageId)` |

`reviewApi.ts`:

| 当前名 | 建议名 |
|--------|--------|
| `getList(productId)` | `getReviews(productId)` |
| `create(productId)` | `createReview(productId)` |

`paymentApi.ts`:

| 当前名 | 建议名 |
|--------|--------|
| `refund(id)` | `refundPayment(id)` |
| `close(id)` | `closePayment(id)` |

---

### 🟡 中优先级

#### FM1. `store/` 与 `stores/` 双目录并存

| 目录 | 文件 | 
|------|------|
| `src/store/` | `authStore.ts`, `uiStore.ts`, `aiStore.ts`, `overlayStore.ts`, `index.ts` |
| `src/stores/` | `chatStore.ts` |

**问题**: 两个目录同样存放 Zustand store，命名不一致导致导入混淆。

**建议**: 统一合并到 `src/store/`（文件数更多一方），或将 `chatStore.ts` 移入。

---

#### FM2. Hook 名跨模块冲突

| Hook | 定义文件 |
|------|---------|
| `useCancelOrder` | `src/hooks/order/useOrders.ts` |
| `useCancelOrder` | `src/admin/hooks/useAdminOrders.ts` |
| `useRefundOrder` | `src/hooks/order/useOrders.ts` |
| `useRefundOrder` | `src/admin/hooks/useAdminOrders.ts` |

**建议**: admin hooks 加 `Admin` 前缀：`useAdminCancelOrder`, `useAdminRefundOrder`

---

#### FM3. Type 名冲突 `UserType`

| 文件 | 定义 |
|------|------|
| `src/types/user.ts` | `type UserType = '00' \| '01' \| '02'` (string literal union) |
| `src/admin/types/admin.ts` | `type UserType = string` (简单别名) |

**建议**: admin 侧的改为 `AdminUserType` 或直接使用 `string` 不导出

---

### 🟢 低优先级

#### FL1. `src/utils/function.ts` 使用 JavaScript 保留字作文件名

JavaScript 的 `function` 是保留关键字，虽然模块系统允许作为文件名，但可能导致工具链警告。

**建议**: 改名 `src/utils/functionUtils.ts`

---

#### FL2. `hooks/` 子目录单复数不一致

`hooks/` 父目录为复数，但子目录全部使用单数：

```
hooks/
├── auth/       # 应为 auths/ 或保持单数
├── chat/       # 应为 chats/
├── order/      # 应为 orders/
├── product/    # 应为 products/
└── ui/         # 应为 uis/ 或保持

管理员端 hooks/ 全部是独立的文件（不用子目录）✅
```

**建议**: 统一全部使用单数（更符合常见实践）或全部使用复数。当前单数模式在 npm 生
态中更常见（如 `lodash/camelCase`），可以保持一致。

---

#### FL3. API 函数参数 `_params` 下划线前缀

`src/api/messageApi.ts:10`:
```typescript
getList(_params?: Record<string, unknown>)
```

`src/api/reviewApi.ts:9`:
```typescript
getList(productId: string, _params?: Record<string, unknown>)
```

**建议**: 移除废弃的参数，或改为 `params` 并实际使用。

---

## 三、汇总统计

| 优先级 | 后端 | 前端 | 合计 |
|--------|------|------|------|
| 🔴 高 | 3 | 2 (含多项) | 5 |
| 🟡 中 | 3 | 3 | 6 |
| 🟢 低 | 1 | 3 | 4 |
| **合计** | **7** | **8** | **15** |

## 四、建议实施顺序

1. **H1** (MessageType 歧义) → 影响编译理解和运行时行为
2. **FH1** (单字母变量 7 处) → 简单安全，可立即执行
3. **M1** (delToken → deleteToken) → 安全重构
4. **M2** (constants→constant 统一) → 仅 user 模块需改
5. **FM1** (store/stores 合并) → 影响导入路径
6. **FH2** (API 方法名 ~15 处) → 需同步更新所有调用方
7. **H2** (IResultCode) → 全局影响，需计划迁移
8. **H3/M3/FM2/FM3/FL1/FL2/FL3** → 后续迭代中逐步改进

---

## 五、无问题确认

以下常见命名区域经扫描确认 ✅ 无问题：

- ✅ Java 类命名 (PascalCase)
- ✅ Java 包命名 (全小写)
- ✅ Java 枚举值 (UPPER_SNAKE_CASE)
- ✅ Java 常量 (UPPER_SNAKE_CASE)
- ✅ React 组件命名 (PascalCase)
- ✅ Hook 命名 (useXxx 前缀)
- ✅ 前端 Type/Interface (PascalCase)
- ✅ 前端常量 (UPPER_SNAKE_CASE)
- ✅ 无前端 snake_case 变量
