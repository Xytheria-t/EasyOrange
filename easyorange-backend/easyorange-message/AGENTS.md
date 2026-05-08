# easyorange-message 模块指南

消息通知模块，混合架构（部分 DDD + 传统分层），支持站内消息、WebSocket 实时推送、消息模板。

> **架构现状**: 本模块尚未完全迁移到 DDD 六边形架构。domain/repository 中存在 MyBatis 实现类
> (`MybatisMessageRepository` 等)，controller/service 层仍为传统分层结构。
> 演进方向：逐步将 MyBatis 实现迁移到 adapter/outbound/persistence/，controller 迁移到 adapter/inbound/。

## 目录结构

```
message/
├── controller/                        # [传统] 控制器 (待迁移到 adapter/inbound/web/)
│   ├── MessageCommandController.java  # 消息写端点
│   ├── MessageQueryController.java    # 消息读端点
│   └── request/                       # 请求 DTO (待迁移到 dto/request/)
├── service/                           # [传统] 服务层 (待迁移到 application/service/)
│   ├── MessageSubscriptionService.java
│   ├── MessageTemplateService.java
│   ├── OfflineMessageService.java
│   └── impl/                          # 实现类
├── dto/                               # DTO
│   ├── request/                       # 请求 DTO
│   │   ├── SendMessageRequest.java
│   │   ├── QueryMessageRequest.java
│   │   ├── SubscriptionRequest.java
│   │   ├── TemplateMessageRequest.java
│   │   └── WsMessage.java
│   └── vo/                            # 视图对象
│       ├── MessageVO.java
│       ├── ConversationVO.java
│       ├── UnreadCountVO.java
│       ├── MessageSubscriptionVO.java
│       └── MessageTemplateVO.java
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
├── domain/                            # [DDD] 领域层
│   ├── event/
│   │   ├── MessageSentEvent.java
│   │   ├── MessageReadEvent.java
│   │   └── MessageDeletedEvent.java
│   ├── repository/                    # ⚠️ 包含 MyBatis 实现类 (应迁移到 adapter/outbound/)
│   │   ├── MessageRepository.java         # 接口
│   │   ├── MybatisMessageRepository.java  # 实现类 (待迁移)
│   │   ├── MessageQueryRepository.java
│   │   ├── MybatisMessageQueryRepository.java # 实现类 (待迁移)
│   │   ├── OfflineMessageRepository.java
│   │   ├── MybatisOfflineMessageRepository.java # 实现类 (待迁移)
│   │   ├── MessageSubscriptionRepository.java
│   │   ├── MybatisMessageSubscriptionRepository.java # 实现类 (待迁移)
│   │   ├── MessageTemplateRepository.java
│   │   └── MybatisMessageTemplateRepository.java # 实现类 (待迁移)
│   ├── service/
│   │   ├── MessageRoutingService.java     # 消息路由
│   │   └── OfflineMessageStoreService.java # 离线消息存储
│   ├── valueobject/
│   │   ├── MessageContent.java
│   │   ├── MessageType.java
│   │   └── Recipient.java
│   └── exception/
│       ├── MessageDomainException.java
│       ├── MessageNotFoundException.java
│       └── UnauthorizedOperationException.java
├── entity/                            # [传统] 实体类 (待迁移到 domain/aggregate + adapter/outbound/persistence/)
│   ├── Message.java
│   ├── MessageSubscription.java
│   ├── MessageTemplate.java
│   └── OfflineMessage.java
├── mapper/                            # [传统] MyBatis Mapper (待迁移到 adapter/outbound/persistence/)
│   ├── MessageMapper.java
│   ├── MessageSubscriptionMapper.java
│   ├── MessageTemplateMapper.java
│   └── OfflineMessageMapper.java
├── enums/
│   ├── MessageStatus.java
│   ├── MessageType.java
│   ├── ReadStatus.java
│   └── MessageResultCode.java
├── constant/
│   └── MessageConstant.java
└── websocket/                         # WebSocket 实时推送
    ├── WebSocketConfig.java               # STOMP 配置
    ├── WebSocketAuthInterceptor.java      # JWT 认证拦截
    ├── WebSocketMessageHandler.java       # 消息处理
    ├── WebSocketNotifier.java             # 实时通知推送
    └── WebSocketEventListener.java        # 连接/断开事件
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

## 演进路线

1. 将 `entity/` 中的实体类拆分：聚合根 → `domain/aggregate/`，数据对象 → `adapter/outbound/persistence/`
2. 将 `mapper/` 迁移到 `adapter/outbound/persistence/mapper/`
3. 将 `domain/repository/` 中的 MyBatis 实现类迁移到 `adapter/outbound/persistence/repository/`
4. 将 `controller/` 迁移到 `adapter/inbound/web/controller/`
5. 将 `service/` 迁移到 `application/service/`
6. 添加 `domain/port/output/` 端口接口

## 常见开发任务

### 添加新消息类型

1. `MessageType` 枚举新增值
2. `MessageRoutingService` 添加路由规则
3. 如需新模板 → `MessageTemplate` 添加记录
4. 添加测试

### 添加 WebSocket 事件

1. `WebSocketMessageHandler` 添加消息类型处理
2. `WebSocketNotifier` 添加推送方法
3. 前端添加对应监听
4. 测试
