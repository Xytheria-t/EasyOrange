# easyorange-message

**Module:** Real-time messaging (WebSocket) + system notifications

## OVERVIEW

WebSocket-based real-time message push, message CRUD, system notifications, scheduled cleanup tasks.

## STRUCTURE

```
easyorange-message/src/main/java/com/cartethyia/easyorange/message/
├── config/              # WebSocketConfig (endpoint registration)
├── constant/            # MessageConstants
├── controller/          # MessageController, NotificationController
├── dto/
│   ├── request/         # Message send/read requests
│   ├── vo/              # MessageVO, NotificationVO
│   └── ws/              # WebSocket message DTOs
├── entity/              # Message, Notification
├── enums/               # MessageType (Private/System/Order)
├── mapper/              # MessageMapper, NotificationMapper
├── service/
│   ├── MessageService.java
│   └── NotificationService.java
├── service/impl/        # Service implementations
├── task/                # @Scheduled cleanup tasks
└── websocket/           # MessageWebSocketHandler
```

## WHERE TO LOOK

| Task | File | Notes |
|------|------|-------|
| WebSocket 连接 | `websocket/MessageWebSocketHandler.java` | 握手、消息收发、心跳 |
| WS 端点注册 | `config/WebSocketConfig.java` | `/ws/message` 端点 |
| 消息 CRUD | `MessageController.java` | 标准 REST |
| 系统通知 | `NotificationController.java` | 推送通知管理 |
| 定时任务 | `task/` | @Scheduled 清理过期消息 |

## CONVENTIONS

- API prefix: `/api/message`
- WebSocket endpoint: `/ws/message`
- 消息类型：Private（私聊）、System（系统通知）、Order（订单相关）
- 通知支持已读/未读状态追踪
- WebSocket 消息使用自定义 WS DTO 格式，不要直接传 Entity

## DEPENDENCIES

```
easyorange-message → easyorange-framework → easyorange-common
```

## COMMANDS

```bash
mvn clean install -pl easyorange-message
```
