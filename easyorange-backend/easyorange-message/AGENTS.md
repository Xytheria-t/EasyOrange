# easyorange-message 模块指南

消息通知模块，DDD 六边形架构，支持站内消息、WebSocket 实时推送。

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
│   │       ├── QueryMessageRequest.java
│   │       └── WsMessage.java
│   ├── inbound/job/                   # 定时任务
│   │   └── MessageArchiveTask.java    # 消息归档/清理（@Scheduled）
│   ├── inbound/websocket/             # WebSocket 入站
│   │   ├── AuthHandshakeHandler.java
│   │   ├── ChatWebSocketHandler.java
│   │   ├── TypingIndicatorService.java
│   │   ├── WebSocketAttributes.java
│   │   ├── WebSocketAuthInterceptor.java
│   │   ├── WebSocketConfig.java
│   │   ├── WebSocketEventConsumer.java
│   │   └── WebSocketEventListener.java
│   └── outbound/
│       ├── persistence/              # 出站持久化 (MyBatis 实现)
│       │   ├── MessageDO.java             # 数据对象 (DO)
│       │   ├── OfflineMessageDO.java
│       │   ├── MessageDataMapper.java     # DO ↔ 聚合根映射
│       │   ├── MessageMapper.java
│       │   ├── OfflineMessageMapper.java
│       │   ├── MessageRepositoryImpl.java
│       │   ├── MessageQueryRepositoryImpl.java
│       │   ├── OfflineMessageRepositoryImpl.java
│       │   └── typehandler/               # 枚举 TypeHandler
│       │       ├── MessageStatusTypeHandler.java
│       │       ├── MessageTypeTypeHandler.java
│       │       └── ReadStatusTypeHandler.java
│       └── websocket/
│           └── WebSocketNotifier.java     # WebSocket 出站推送
├── application/                       # [DDD] 应用层 (CQRS)
│   ├── config/
│   │   └── MessageDomainConfig.java        # @Bean 方式注册服务（保持 domain 层纯净）
│   ├── service/
│   │   ├── RateLimiterService.java           # 消息发送频率限制（应用层运维策略）
│   │   └── OfflineMessageStoreService.java   # 离线消息存储（应用层编排：在线则存、离线则略）
│   ├── command/                        # 命令处理
│   │   ├── MessageCommandHandler.java
│   │   ├── SendMessageCommand.java
│   │   ├── SendSystemMessageCommand.java
│   │   ├── MarkAsReadCommand.java
│   │   ├── MarkAsReadBatchCommand.java
│   │   ├── DeleteMessageCommand.java
│   │   └── RecallMessageCommand.java
│   └── query/                         # 查询处理
│       ├── MessageQueryHandler.java
│       ├── ConversationQueryHandler.java
│       ├── MessageQuery.java
│       └── dto/                      # 查询返回 VO
│           ├── ConversationListVO.java
│           ├── ConversationVO.java
│           ├── MessageVO.java
│           └── UnreadCountVO.java
├── domain/                            # [DDD] 领域层
│   ├── aggregate/                     # 聚合根
│   │   ├── Message.java
│   │   └── OfflineMessage.java
│   ├── event/                         # 领域事件
│   │   ├── MessageSentEvent.java
│   │   ├── MessageReadEvent.java
│   │   ├── MessageDeletedEvent.java
│   │   └── MessageRecalledEvent.java
│   ├── port/                          # 端口接口（隔离外部依赖）
│   │   ├── MessageNotifierPort.java
│   │   └── UserInfoPort.java
│   ├── repository/                    # 仓储接口 (实现已迁移到 adapter/outbound/)
│   │   ├── MessageRepository.java
│   │   ├── OfflineMessageRepository.java
│   │   └── query/
│   │       └── MessageQueryRepository.java
│   ├── service/                       # 领域服务（纯领域规则，无仓储编排）
│   │   └── SensitiveWordFilterService.java   # 消息内容敏感词过滤
│   ├── constant/
│   │   └── MessageConstant.java
│   ├── enums/
│   │   ├── MessageResultCode.java
│   │   ├── MessageStatus.java
│   │   ├── MessageType.java
│   │   └── ReadStatus.java
│   ├── valueobject/
│   │   ├── MessageQuery.java
│   │   ├── UnreadCount.java
│   │   └── UserInfo.java
│   └── exception/
│       ├── MessageDomainException.java
│       ├── MessageNotFoundException.java
│       └── UnauthorizedOperationException.java
```

## WebSocket 架构

- 协议: STOMP over WebSocket
- 认证: `WebSocketAuthInterceptor` 从 STOMP Header 提取 JWT Token
- 推送: `WebSocketNotifier` 向在线用户实时推送消息
- 离线: `OfflineMessageStoreService` 存储离线消息，上线后重推

## 消息路由

推送判定内聚在 `MessageCommandHandler`（SendMessageCommand 路径）：
- 在线（`MessageNotifierPort.isUserOnline`）→ `WebSocketNotifier` 实时推送
- 离线 → `OfflineMessageStoreService.storeIfOffline` 存为离线消息，上线后重推

## 安全要点

- WebSocket 连接必须 JWT 认证
- 消息标题/内容原样存储，XSS 防护在渲染端文本输出（前端 `escapeHtml` 处理，聚合根不转义）
- 用户只能读取/删除自己的消息
- 消息发送限流

## 消息归档服务

`MessageArchiveTask`（`adapter/inbound/job/`）提供定时归档和清理功能：

- **清理任务**: 每天凌晨 3 点清理超过保留天数的消息
- **归档任务**: 每月 1 号凌晨 2 点将旧消息归档到 `eo_message_archive` 表
- **配置项**: `easyorange.message.retention-days`（默认 90 天）
- **批次大小**: 每次处理 1000 条记录

## 演进路线

> 历史 5 步迁移（repository/entity/controller/service/port 全量 DDD 化）已全部完成，模块已为完整 DDD 六边形架构（2026-07-31 收口）。

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
