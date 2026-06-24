# easyorange-message 模块指南

消息通知模块，混合架构（部分 DDD + 传统分层），支持站内消息、WebSocket 实时推送、消息模板。

> **架构现状**: MyBatis Repository 实现已迁移到 `adapter/outbound/persistence/`。Controller 层已迁移到 `adapter/inbound/web/controller/`，service 层仍为传统分层结构，待进一步演进。

## 目录结构

```
message/
├── adapter/inbound/web/controller/    # REST 控制器
│   ├── MessageCommandController.java
│   └── MessageQueryController.java
├── adapter/                           # 适配器层
│   └── outbound/persistence/          # 出站适配器 (MyBatis 实现)
│       ├── MessageMapper.java
│       ├── MessageSubscriptionMapper.java
│       ├── MessageTemplateMapper.java
│       ├── OfflineMessageMapper.java
│       ├── MybatisMessageRepository.java
│       ├── MybatisMessageQueryRepository.java
│       ├── MybatisMessageSubscriptionRepository.java
│       ├── MybatisMessageTemplateRepository.java
│       └── MybatisOfflineMessageRepository.java
├── application/                       # [DDD] 应用层 (CQRS)
│   ├── command/
│   │   ├── MessageCommandHandler.java
│   │   ├── SendMessageCommand.java
│   │   ├── SendSystemMessageCommand.java
│   │   ├── MarkAsReadCommand.java
│   │   ├── MarkAsReadBatchCommand.java
│   │   └── DeleteMessageCommand.java
│   └── query/
│       ├── MessageQueryHandler.java
│       ├── ConversationQueryHandler.java
│       ├── MessageQuery.java
│       └── UnreadCountQuery.java
├── service/                           # [传统] 服务层 (待迁移到 application/service/)
│   ├── config/
│   │   └── MessageDomainServiceConfig.java # @Bean 方式注册领域服务（保持 domain 层纯净）
│   ├── MessageArchiveService.java     # 消息归档定时服务
│   ├── MessageSubscriptionService.java
│   ├── MessageTemplateService.java
│   ├── OfflineMessageService.java
│   └── impl/
├── dto/
│   ├── request/
│   │   ├── SendMessageRequest.java
│   │   ├── QueryMessageRequest.java
│   │   ├── SubscriptionRequest.java
│   │   ├── TemplateMessageRequest.java
│   │   └── WsMessage.java
│   └── vo/
│       ├── MessageResponse.java
│       ├── ConversationResponse.java
│       ├── UnreadCountResponse.java
│       ├── MessageSubscriptionResponse.java
│       └── MessageTemplateResponse.java
├── domain/                            # [DDD] 领域层
│   ├── event/
│   │   ├── MessageSentEvent.java
│   │   ├── MessageReadEvent.java
│   │   └── MessageDeletedEvent.java
│   ├── repository/                    # 仓储接口 (实现已迁移到 adapter/outbound/)
│   │   ├── MessageRepository.java
│   │   ├── MessageQueryRepository.java
│   │   ├── OfflineMessageRepository.java
│   │   ├── MessageSubscriptionRepository.java
│   │   └── MessageTemplateRepository.java
│   ├── service/
│   │   ├── MessageRoutingService.java        # 根据订阅偏好路由消息（在线推送/离线存储）
│   │   ├── OfflineMessageStoreService.java   # 离线消息存储和重推
│   │   ├── RateLimiterService.java           # 消息发送频率限制
│   │   └── SensitiveWordFilterService.java   # 消息内容敏感词过滤
│   ├── valueobject/
│   │   ├── MessageContent.java
│   │   ├── MessageContentFormat.java
│   │   └── Recipient.java
│   └── exception/
│       ├── MessageDomainException.java
│       ├── MessageNotFoundException.java
│       └── UnauthorizedOperationException.java
├── entity/                            # [传统] 实体类 (待迁移)
│   ├── Message.java
│   ├── MessageSubscription.java
│   ├── MessageTemplate.java
│   └── OfflineMessage.java
├── enums/
│   ├── MessageStatus.java
│   ├── MessageType.java
│   ├── ReadStatus.java
│   ├── MessageResultCode.java
│   └── OfferMessageType.java          # 议价消息类型: OFFER(6), OFFER_ACCEPTED(7), OFFER_REJECTED(8), COUNTER_OFFER(9)
├── constant/
│   └── MessageConstant.java
├── domain/port/
│   └── OfferProcessingPort.java       # 议价处理端口 (WebSocket → product 模块)
└── websocket/                         # WebSocket 实时推送
    ├── WebSocketConfig.java
    ├── WebSocketAuthInterceptor.java
    ├── WebSocketNotifier.java
    └── WebSocketEventListener.java
```

## WebSocket 架构

- 协议: STOMP over WebSocket
- 认证: `WebSocketAuthInterceptor` 从 STOMP Header 提取 JWT Token
- 推送: `WebSocketNotifier` 向在线用户实时推送消息
- 离线: `OfflineMessageStoreService` 存储离线消息，上线后重推

### 议价协议 (WebSocket)

通过 STOMP over WebSocket 支持 AI 托管寄售的实时议价：

| 消息类型 | code | 说明 | 方向 |
|---------|------|------|------|
| OFFER | 6 | 买家发起出价 | Client → Server |
| OFFER_ACCEPTED | 7 | AI 接受出价 | Server → Client |
| OFFER_REJECTED | 8 | AI 拒绝出价 | Server → Client |
| COUNTER_OFFER | 9 | AI 发起还价 | Server → Client |

- 议价端点: `@MessageMapping("/offer.make")` → `OfferProcessingPort` → product 模块 `OfferAppService`
- 消息格式: `WsMessage` 的 `type` 字段标识消息类型

## 消息路由

`MessageRoutingService` 根据用户订阅偏好决定推送方式：
- 在线 → WebSocket 实时推送
- 离线 → 存储为离线消息，上线后推送
- 订阅检查 → 用户可关闭某类消息通知

## 安全要点

- WebSocket 连接必须 JWT 认证
- 消息内容 XSS 过滤 (`HtmlUtils.htmlEscape`)
- 用户只能读取/删除自己的消息
- 消息发送限流

## 消息归档服务

`MessageArchiveService` 提供定时归档和清理功能：

- **清理任务**: 每天凌晨 3 点清理超过保留天数的消息
- **归档任务**: 每月 1 号凌晨 2 点将旧消息归档到 `eo_message_archive` 表
- **配置项**: `easyorange.message.retention-days`（默认 90 天）
- **批次大小**: 每次处理 1000 条记录

## 演进路线

1. ~~将 `domain/repository/` 中的 MyBatis 实现类迁移到 `adapter/outbound/persistence/`~~ ✅ 已完成
2. 将 `entity/` 中的实体类拆分：聚合根 → `domain/aggregate/`，数据对象 → `adapter/outbound/persistence/`
3. ~~将 `controller/` 迁移到 `adapter/inbound/web/controller/`~~ ✅ 已完成
4. 将 `service/` 迁移到 `application/service/`
5. 添加 `domain/port/` 端口接口

## 常见开发任务

### 添加新消息类型

1. `MessageType` 枚举新增值
2. `MessageRoutingService` 添加路由规则
3. 如需新模板 → `MessageTemplate` 添加记录
4. 添加测试

### 添加 WebSocket 事件

1. `ChatWebSocketHandler` 添加消息类型处理
2. `WebSocketNotifier` 添加推送方法
3. 前端添加对应监听
4. 测试
