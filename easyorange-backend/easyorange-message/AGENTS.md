# easyorange-message 模块指南

消息通知模块，DDD 六边形架构，支持站内消息、WebSocket 实时推送、消息模板。

> **架构现状**: 全模块已完成 DDD 分层迁移——聚合根在 `domain/aggregate/`（原 `entity/` 已消除），MyBatis Repository 实现在 `adapter/outbound/persistence/`（DO + Mapper + RepositoryImpl + typehandler），Controller 在 `adapter/inbound/web/controller/`，入站请求 DTO 在 `adapter/inbound/web/dto/request/`，应用层（CQRS + 定时服务 + 领域服务注册）在 `application/`，端口接口在 `domain/port/`。无分层的 `service/` / `entity/` / `dto/` 目录均已消除。

## 目录结构

```
message/
├── adapter/                           # 适配器层（六边形出/入站）
│   ├── inbound/web/
│   │   ├── controller/                # REST 控制器
│   │   │   ├── MessageCommandController.java
│   │   │   └── MessageQueryController.java
│   │   └── dto/request/               # 入站请求 DTO
│   │       ├── SendMessageRequest.java
│   │       ├── QueryMessageRequest.java
│   │       ├── SubscriptionRequest.java
│   │       ├── TemplateMessageRequest.java
│   │       └── WsMessage.java
│   └── outbound/persistence/          # 出站适配器 (MyBatis 实现)
│       ├── MessageDO.java             # 数据对象 (DO)
│       ├── MessageSubscriptionDO.java
│       ├── MessageTemplateDO.java
│       ├── OfflineMessageDO.java
│       ├── MessageDataMapper.java     # DO ↔ 聚合根映射
│       ├── MessageMapper.java
│       ├── MessageSubscriptionMapper.java
│       ├── MessageTemplateMapper.java
│       ├── OfflineMessageMapper.java
│       ├── MessageRepositoryImpl.java
│       ├── MessageQueryRepositoryImpl.java
│       ├── MessageSubscriptionRepositoryImpl.java
│       ├── MessageTemplateRepositoryImpl.java
│       ├── OfflineMessageRepositoryImpl.java
│       └── typehandler/               # 枚举 TypeHandler
│           ├── MessageStatusTypeHandler.java
│           ├── MessageTypeTypeHandler.java
│           └── ReadStatusTypeHandler.java
├── application/                       # [DDD] 应用层 (CQRS)
│   ├── config/
│   │   └── MessageDomainServiceConfig.java # @Bean 方式注册领域服务（保持 domain 层纯净）
│   ├── service/
│   │   ├── MessageArchiveService.java       # 消息归档定时服务（@Scheduled）
│   │   └── RateLimiterService.java           # 消息发送频率限制（应用层运维策略）
│   ├── command/
│   │   ├── MessageCommandHandler.java
│   │   ├── SendMessageCommand.java
│   │   ├── SendSystemMessageCommand.java
│   │   ├── MarkAsReadCommand.java
│   │   ├── MarkAsReadBatchCommand.java
│   │   ├── DeleteMessageCommand.java
│   │   └── RecallMessageCommand.java
│   └── query/
│       ├── MessageQueryHandler.java
│       ├── ConversationQueryHandler.java
│       ├── MessageQuery.java
│       └── dto/                      # 查询返回 VO
│           ├── ConversationListVO.java
│           ├── ConversationVO.java
│           ├── MessageVO.java
│           ├── MessageSubscriptionVO.java
│           ├── MessageTemplateVO.java
│           └── UnreadCountVO.java
├── domain/                            # [DDD] 领域层
│   ├── aggregate/                     # 聚合根
│   │   ├── Message.java
│   │   ├── MessageSubscription.java
│   │   ├── MessageTemplate.java
│   │   └── OfflineMessage.java
│   ├── event/
│   │   ├── MessageSentEvent.java
│   │   ├── MessageReadEvent.java
│   │   ├── MessageDeletedEvent.java
│   │   └── MessageRecalledEvent.java
│   ├── port/                          # 端口接口（隔离外部依赖）
│   │   ├── MessageNotifierPort.java
│   │   └── UserInfoPort.java
│   ├── repository/                    # 仓储接口 (实现已迁移到 adapter/outbound/)
│   │   ├── MessageRepository.java
│   │   ├── MessageSubscriptionRepository.java
│   │   ├── MessageTemplateRepository.java
│   │   ├── OfflineMessageRepository.java
│   │   └── query/
│   │       └── MessageQueryRepository.java
│   ├── service/
│   │   ├── MessageRoutingService.java        # 根据订阅偏好路由消息（在线推送/离线存储）
│   │   ├── OfflineMessageStoreService.java   # 离线消息存储和重推
│   │   └── SensitiveWordFilterService.java   # 消息内容敏感词过滤
│   ├── valueobject/
│   │   ├── MessageContent.java
│   │   ├── MessageContentFormat.java
│   │   ├── Recipient.java
│   │   ├── MessageQuery.java
│   │   ├── UnreadCount.java
│   │   └── UserInfo.java
│   └── exception/
│       ├── MessageDomainException.java
│       ├── MessageNotFoundException.java
│       └── UnauthorizedOperationException.java
├── enums/
│   ├── MessageStatus.java
│   ├── MessageType.java
│   ├── ReadStatus.java
│   └── MessageResultCode.java
├── constant/
│   └── MessageConstant.java
└── websocket/                         # WebSocket 实时推送
    ├── WebSocketConfig.java
    ├── WebSocketAuthInterceptor.java
    ├── WebSocketNotifier.java
    ├── WebSocketEventListener.java
    ├── WebSocketEventConsumer.java
    ├── ChatWebSocketHandler.java
    └── TypingIndicatorService.java
```

## WebSocket 架构

- 协议: STOMP over WebSocket
- 认证: `WebSocketAuthInterceptor` 从 STOMP Header 提取 JWT Token
- 推送: `WebSocketNotifier` 向在线用户实时推送消息
- 离线: `OfflineMessageStoreService` 存储离线消息，上线后重推

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
2. ~~将 `entity/` 中的实体类拆分：聚合根 → `domain/aggregate/`，数据对象 → `adapter/outbound/persistence/`~~ ✅ 已完成（`entity/` 目录已消除，DO 在 `adapter/outbound/persistence/`，聚合根在 `domain/aggregate/`）
3. ~~将 `controller/` 迁移到 `adapter/inbound/web/controller/`~~ ✅ 已完成
4. ~~将 `service/` 迁移到 `application/service/`~~ ✅ 已完成（2026-07-31，死代码清理 + MessageArchiveService/Config 迁入 application/）
5. ~~添加 `domain/port/` 端口接口~~ ✅ 已完成（`MessageNotifierPort` / `UserInfoPort`）

> 演进路线 5 步全部完成，模块已为完整 DDD 六边形架构。

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
