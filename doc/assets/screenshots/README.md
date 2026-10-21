# 界面截图

> 面试时可直接翻这八张，不用现场起环境。全部用 dev 种子数据（`scripts/dev-up.sh` 起栈后）截取，账号 `testuser` / `admin`，口令 `Password123`。

| 文件 | 页面 | 看点 |
|------|------|------|
| `01-home.jpg` | 首页首屏 | 两句话讲清定位：Agent 编排 × RAG × 评估闭环 · DDD + 六边形 + 事件驱动可靠性 |
| `02-home-recommend.jpg` | 首页「精选推荐」 | 商品卡片带 AI 托管中 / AI 热度精选标签（发布助手产出的字段回流到展示层） |
| `03-publish-assistant.jpg` | 发布助手 | 拍照识别 → 多步查证链产出属性 / 建议价 / 标题描述 |
| `04-ai-chat-playground.jpg` | AI 找货（Playground） | 多轮 Agent 对话 + 步骤可视化 + 引用溯源 |
| `05-admin-overview.jpg` | 管理端 · 数据总览 | 一屏看全用户 / 商品 / 订单 / 营收四个口径 + 分布与走势 |
| `06-admin-product-audit.jpg` | 管理端 · 商品审核 | 批量审核 + 审核日志（管理端六边形的落地面） |
| `07-admin-order-intervention.jpg` | 管理端 · 订单干预 | 详情弹窗 + 取消 / 强制完成 / 退款（按订单状态给可执行动作） |
| `08-admin-user-detail.jpg` | 管理端 · 用户详情 | 风控字段 + 解锁 / 强制下线 / 改角色 |

**重拍方法**：`scripts/dev-up.sh` 起栈 → Playwright 登录后逐路由 `page.screenshot()`。整页截图会把「滚动显现」区块拍成空白，所以首页分两张视口图。
