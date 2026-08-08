# API 速查

> 本文档汇总后端所有 REST API 和 WebSocket 端点，便于快速查阅。
> 详细业务机制见各专题文档，架构规范见 [doc/架构/](../架构/)。

---

## 一、用户与认证

| 功能 | 方法+路径 | 鉴权 |
|------|----------|------|
| 发送验证码 | `POST /api/auth/sms-code` | 否 |
| 注册 | `POST /api/auth/register` | 否 |
| 登录 | `POST /api/auth/login` | 否 |
| 刷新 Token | `POST /api/auth/refresh` | Refresh Token |
| 登出 | `POST /api/auth/logout` | Access Token |
| 获取当前用户 | `GET /api/users/me` | Access Token |
| 更新个人资料 | `PUT /api/users/me` | Access Token |
| 密码重置（忘记密码） | `POST /api/auth/password/reset` | 否 |
| 修改密码（已登录） | `PUT /api/auth/password/change` | Access Token |

## 二、商品

| 功能 | 方法+路径 | 鉴权 |
|------|----------|------|
| 提交资产 | `POST /api/products` | Access Token |
| 资产详情 | `GET /api/products/{id}` | 否 |
| 资产列表（分页） | `GET /api/products` | 否 |
| 编辑资产 | `PUT /api/products/{id}` | Access Token（资产方） |
| 提交审核 | `PUT /api/products/{id}/submit` | Access Token（资产方） |
| 直接上架 | `PUT /api/products/{productId}/online` | ADMIN |
| 下架 | `PUT /api/products/{productId}/offline` | Access Token（资产方） |
| 上传图片 | `POST /api/uploads/image` | Access Token |

详见 [AI-资产管理.md](./AI-资产管理.md) 的 WebSocket 协议。

## 三、搜索

| 功能 | 方法+路径 | 鉴权 |
|------|----------|------|
| 全文搜索 | `GET /api/products/search` | 否 |
| 语义搜索 | `GET /api/ai/semantic-search` | 否 |

## 四、订单

| 功能 | 方法+路径 | 鉴权 |
|------|----------|------|
| 创建订单 | `POST /api/orders` | Access Token |
| 订单列表（通用） | `GET /api/orders` | Access Token |
| 订单详情 | `GET /api/orders/owned/{id}` | Access Token（双方） |
| 我的订单（买入） | `GET /api/orders/my` | Access Token |
| 卖出订单 | `GET /api/orders/sold` | Access Token |
| 支付订单 | `PUT /api/orders/{id}/pay` | Access Token（认领方） |
| 取消订单 | `PUT /api/orders/{id}/cancel` | Access Token（认领方） |
| 发货 | `PUT /api/orders/{id}/ship` | Access Token（资产方） |
| 确认收货 | `PUT /api/orders/{id}/receive` | Access Token（认领方） |
| 退款 | `PUT /api/orders/{id}/refund` | Access Token（认领方） |

## 五、支付

| 功能 | 方法+路径 | 鉴权 |
|------|----------|------|
| 发起支付 | `POST /api/payments` | Access Token |
| 支付详情 | `GET /api/payments/{id}` | Access Token |
| 支付方式列表 | `GET /api/payment-methods` | 否 |

## 六、互动

| 功能 | 方法+路径 | 鉴权 |
|------|----------|------|
| 收藏 | `POST /api/favorites` | Access Token |
| 取消收藏 | `DELETE /api/favorites/{productId}` | Access Token |
| 我的收藏 | `GET /api/favorites` | Access Token |
| 站内消息列表 | `GET /api/messages` | Access Token |
| 实时聊天 | WebSocket `/ws/chat` | STOMP Token |

## 七、AI 功能

| 功能 | 方法+路径 | 参数 |
|------|----------|------|
| 智能估值 | `POST /api/ai/pricing` | productName, description, categoryName, conditionLevel |
| 拍照上架 | `POST /api/ai/auto-listing` | imageFile (multipart) |
| AI 审核 | `POST /api/ai/review` | productId |
| 智能问答 | `POST /api/ai/qa` | productId, question |
| 智能文案 | `POST /api/ai/generate-copy` | productInfo + style (standard/detailed/concise/emotional) |
| 语义搜索 | `GET /api/ai/semantic-search` | keyword, pageNum, pageSize |
| AI 议价 | ~~已下线~~ | — | — |
| 智能导购搜索 | `GET /api/ai/guide-search` | query, filters |

## 八、信用

| 功能 | 方法+路径 | 鉴权 |
|------|----------|------|
| 我的信用 | `GET /api/credit/my` | Access Token |
| 信用详情 | `GET /api/credit/detail/{userId}` | Access Token |
| 重新计算 | `POST /api/credit/recalculate` | Access Token（自己） |

## 九、举报

**用户侧**：
| 功能 | 方法+路径 |
|------|----------|
| 提交举报 | `POST /api/reports/product/{id}` |
| 我的举报列表 | `GET /api/reports/my` |
| 举报详情 | `GET /api/reports/{id}` |

> 24h 内同一用户对同一商品不可重复举报（`eo_product_report` 表有唯一约束）。

**管理端**（`/api/admin/reports/*`）：
| 功能 | 方法+路径 |
|------|----------|
| 举报列表 | `GET /api/admin/reports` |
| 举报详情 | `GET /api/admin/reports/{id}` |
| 处理单条 | `PUT /api/admin/reports/{id}/handle` |
| 批量处理 | `PUT /api/admin/reports/batch-handle` |
| 处理历史 | `GET /api/admin/reports/{id}/history` |
| 统计 | `GET /api/admin/reports/stats` |

## 十、管理端

| 功能 | 方法+路径 |
|------|----------|
| 商品列表 | `GET /api/admin/products` |
| 商品审核 | `PUT /api/admin/products/{id}/audit` |
| AI 审核建议 | `GET /api/admin/products/{id}/ai-review` |
| 用户管理 | `GET/PUT /api/admin/users` |
| 分类管理 | `GET/POST/PUT/DELETE /api/admin/categories` |
| 评价管理 | `GET/DELETE /api/admin/reviews` |
| 订单管理 | `GET /api/admin/orders` |
| 仪表盘统计 | `GET /api/admin/dashboard` |

---

**相关文档**：
- 架构 [doc/架构/](../架构/)
- AI 能力清单 [AI-资产管理.md](./AI-资产管理.md)
- 顶层规则 [AGENTS.md](../../AGENTS.md)
