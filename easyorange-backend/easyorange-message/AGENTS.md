# easyorange-message 模块指南

消息通知模块，DDD 六边形架构，支持站内消息与 WebSocket 实时推送。

## 目录结构

```
message/
├── adapter/
│   ├── inbound/
│   │   ├── config/MessageRetentionProperties.java   # 保留/宽限天数（easyorange.message.*）
│   │   ├── job/                       # MessageArchiveTask（@Scheduled 归档/清理）+ MessageArchiveBatchHandler
│   │   ├── web/
│   │   │   ├── controller/            # MessageCommandController / MessageQueryController
│   │   │   └── dto/request/           # QueryMessageRequest / WsMessage
│   │   └── websocket/                 # WebSocketConfig / WebSocketAuthInterceptor / AuthHandshakeHandler
│   │                                  #   / ChatWebSocketHandler / TypingIndicatorService / WebSocketAttributes
│   │                                  #   / WebSocketEventConsumer / WebSocketEventListener
│   └── outbound/
│       ├── persistence/               # MessageDO / OfflineMessageDO / MessageDataMapper / MessageMapper
│       │                              #   / OfflineMessageMapper / MessageRepositoryImpl / OfflineMessageRepositoryImpl
│       │   └── query/MessageQueryRepositoryImpl.java
│       └── websocket/WebSocketNotifier.java   # MessageNotifierPort 实现（在线判定 + /queue/notification 推送）
├── application/
│   ├── config/MessageDomainConfig.java        # @Bean 注册领域服务（保持 domain 层纯净）
│   ├── service/                       # OfflineMessageStoreService / SystemNotificationPayload
│   ├── command/                       # MessageCommandHandler + SendMessage / SendSystemMessage / MarkAsRead
│   │                                  #   / MarkAsReadBatch / DeleteMessage / RecallMessage 命令
│   ├── query/                         # MessageQueryHandler / ConversationQueryHandler
│   │   └── dto/                       # ConversationListVO / ConversationVO / MessageVO / UnreadCountVO
│   └── port/query/MessageQueryRepository.java  # 读仓储（读模型是 application 层概念）
└── domain/
    ├── aggregate/                     # Message / OfflineMessage
    ├── constant/MessageConstant.java
    ├── enums/                         # MessageResultCode / MessageStatus / MessageType / ReadStatus / PushStatus
    ├── event/MessageRecalledEvent.java
    ├── exception/MessageDomainException.java
    ├── port/                          # MessageNotifierPort / UserInfoPort
    ├── repository/                    # MessageRepository / OfflineMessageRepository
    ├── service/SensitiveWordFilterService.java  # 消息内容敏感词过滤
    └── valueobject/                   # MessageQuery / UnreadCount / UserInfo
```

## WebSocket 架构

- 协议: STOMP over WebSocket
- 认证: `WebSocketAuthInterceptor` 从 STOMP Header 提取 JWT Token
- 聊天实时帧由入站 `ChatWebSocketHandler` 发送（`/queue/chat/{conversationId}` 会话帧 + `/queue/unread-count` 未读数）
- `WebSocketNotifier`（`MessageNotifierPort`）负责在线判定与系统通知推送（`/queue/notification`）
- 撤回广播: `MessageRecalledEvent` → `WebSocketEventConsumer`（队列 `eo.message.websocket`）
- 离线: `OfflineMessageStoreService` 落 `OfflineMessage`（PENDING），`replayPending` 上线后补推系统通知

## 消息路由

REST 与 WebSocket 共用 `MessageCommandHandler`（限流唯一裁决点，避免双重计数）：

- 发送前经 `SensitiveWordFilterService.filter` 过滤标题/内容，再保存 `Message`
- 接收方离线（`MessageNotifierPort.isUserOnline` 为 false）→ `OfflineMessageStoreService.storeIfOffline` 落离线消息，上线后重推；在线聊天的实时帧由 `ChatWebSocketHandler` 推回会话频道

## 安全要点

- WebSocket 连接必须 JWT 认证
- 消息发送限流: `MessageCommandHandler` + framework `DistributedRateLimiter`（5 条/秒/用户，Redis 不可用 fail-open）
- 标题/内容经敏感词过滤后存储；XSS 防护在渲染端文本输出（前端 `escapeHtml` 处理，聚合根不转义）
- 用户只能读取/删除自己的消息

## 消息归档服务

`MessageArchiveTask`（`adapter/inbound/job/`）提供定时归档和清理功能：

- **清理任务**: 每天凌晨 3 点清理「保留天数 + 宽限期」之前的消息（分批 DELETE 1000 条）
- **归档任务**: 每月 1 号凌晨 2 点把超期消息归档到 `eo_message_archive`（`MessageArchiveBatchHandler` 分批原子搬移）
- **配置项**: `easyorange.message.retention-days`（默认 90 天）/ `cleanup-grace-days`（默认 35 天，宽限期须大于归档周期）

## 常见开发任务

### 添加新消息类型

1. `MessageType` 枚举新增值
2. 如需新推送/离线逻辑 → 在 `MessageCommandHandler` 的路由判定处处理
3. 添加测试

### 添加 WebSocket 事件

1. `ChatWebSocketHandler` 添加消息类型处理
2. `WebSocketNotifier` 添加推送方法
3. 前端添加对应监听
4. 测试
