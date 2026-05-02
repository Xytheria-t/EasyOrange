# easyorange-message Module Agents

Professional agent configuration for the messaging and notification module.

## Module Overview

The `easyorange-message` module handles all messaging functionality including:
- Internal messaging (user-to-user, system notifications)
- WebSocket real-time messaging (STOMP protocol)
- Message template management
- Message subscription preferences
- Offline message storage and retry
- Unread message counting
- CQRS for message commands and queries

## Available Agents

### 1. **message-websocket-agent**

**Purpose**: Handle WebSocket real-time messaging

**When to use**:
- Implementing new WebSocket endpoints
- Modifying message routing logic
- Adding real-time notifications
- Optimizing WebSocket performance

**Capabilities**:
- STOMP protocol configuration
- WebSocket message handlers
- Real-time push notifications
- Connection management

**Example**:
```
"Add WebSocket endpoint for typing indicators"
"Implement real-time notification broadcasting"
"Add WebSocket connection pooling"
```

### 2. **message-routing-agent**

**Purpose**: Handle message routing and delivery

**When to use**:
- Adding new message types
- Modifying message delivery rules
- Implementing message filtering
- Adding subscription-based routing

**Capabilities**:
- Message routing service
- Online/offline detection
- Subscription preference checking
- Multi-channel delivery

**Example**:
```
"Add message routing by user preferences"
"Implement priority message queue"
"Add message filtering by content type"
```

### 3. **message-offline-agent**

**Purpose**: Handle offline message storage and retry

**When to use**:
- Implementing offline storage
- Adding retry mechanisms
- Optimizing offline message cleanup
- Handling message delivery guarantees

**Capabilities**:
- Offline message persistence
- Retry scheduling
- Message expiration
- Delivery confirmation tracking

**Example**:
```
"Add offline message storage for push notifications"
"Implement exponential backoff retry"
"Add message delivery confirmation"
```

### 4. **message-template-agent**

**Purpose**: Handle message template management

**When to use**:
- Adding new message templates
- Implementing template rendering
- Adding multi-language support
- Template variable validation

**Capabilities**:
- Template CRUD operations
- Template rendering engine
- Variable substitution
- Template versioning

**Example**:
```
"Add email template for order confirmation"
"Implement template variable validation"
"Add multi-language template support"
```

## Agent Usage Patterns

### Standard Development Workflow

```
1. Identify the feature/bug
   ↓
2. Choose appropriate agent
   ↓
3. Agent analyzes existing patterns
   ↓
4. Agent implements following TDD
   ↓
5. Code review with java-code-reviewer + security-reviewer
   ↓
6. Test and verify
```

### Agent Selection Matrix

| Task Type | Primary Agent | Secondary Agent |
|-----------|--------------|-----------------|
| Real-time messaging | message-websocket-agent | message-routing-agent |
| Message delivery | message-routing-agent | message-offline-agent |
| Offline support | message-offline-agent | message-websocket-agent |
| Template system | message-template-agent | message-routing-agent |
| Notification system | message-routing-agent | message-websocket-agent |
| Message history | message-offline-agent | message-template-agent |

## Architecture Patterns

### WebSocket + CQRS Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  REST Controllers    │    │  WebSocket Layer         │  │
│  │  - MessageCommandController                     │  │
│  │  - MessageQueryController  │    - WebSocketMessageHandler│  │
│  └──────────────────────┘    │    - WebSocketNotifier     │  │
│                              └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                  Application Layer                           │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  Command Handlers    │    │  Query Handlers          │  │
│  │  - MessageCommandHandler  │  - MessageQueryHandler   │  │
│  │  - SendMessage       │    │  - GetUnreadCount        │  │
│  │  - MarkAsRead        │    │  - ListMessages          │  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                    Domain Layer                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Message (Entity with behavior)                      │  │
│  │  - create(), send(), read(), delete()                │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  Domain Services                                     │  │
│  │  - MessageRoutingService                             │  │
│  │  - OfflineMessageStoreService                        │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  Domain Events                                       │  │
│  │  - MessageSentEvent, MessageReadEvent, etc.          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│               Infrastructure Layer                           │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  Persistence         │    │  WebSocket Config        │  │
│  │  - MybatisMessageRepo│    │  - WebSocketConfig       │  │
│  │  - MybatisOfflineRepo│    │  - WebSocketAuthInterceptor│  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Directory Structure

```
message/
├── application/
│   ├── command/
│   │   ├── MessageCommandHandler.java
│   │   ├── SendMessageCommand.java
│   │   └── ...
│   └── query/
│       ├── MessageQueryHandler.java
│       └── ...
├── controller/
│   ├── MessageCommandController.java
│   └── MessageQueryController.java
├── domain/
│   ├── event/
│   │   ├── MessageSentEvent.java
│   │   └── ...
│   ├── repository/
│   │   ├── MessageRepository.java
│   │   └── query/
│   ├── service/
│   │   ├── MessageRoutingService.java
│   │   └── OfflineMessageStoreService.java
│   └── valueobject/
│       ├── MessageContent.java
│       └── ...
├── entity/
│   ├── Message.java
│   ├── MessageTemplate.java
│   └── OfflineMessage.java
├── mapper/
│   ├── MessageMapper.java
│   └── ...
├── service/
│   ├── MessageSubscriptionService.java
│   ├── MessageTemplateService.java
│   └── OfflineMessageService.java
└── websocket/
    ├── WebSocketConfig.java
    ├── WebSocketMessageHandler.java
    ├── WebSocketNotifier.java
    └── WebSocketAuthInterceptor.java
```

## Code Conventions

### Message Entity with Behavior

```java
public class Message {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private MessageContent content;
    private MessageStatus status;

    public void send() {
        this.status = MessageStatus.SENT;
        registerEvent(new MessageSentEvent(this));
    }

    public void read() {
        this.status = MessageStatus.READ;
        registerEvent(new MessageReadEvent(this));
    }
}
```

### WebSocket Security

```java
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // Validate JWT token from STOMP headers
        // Reject unauthorized connections
    }
}
```

### XSS Prevention

```java
public static String sanitizeContent(String content) {
    return HtmlUtils.htmlEscape(content);
}
```

## Security Checklist

- [ ] WebSocket connections authenticated via JWT
- [ ] Message content sanitized (XSS prevention)
- [ ] Users can only read/delete their own messages
- [ ] Rate limiting on message sending
- [ ] Sensitive data not logged
- [ ] Offline messages encrypted at rest

## Testing Requirements

- **Unit Tests**: Message entity behavior, Routing logic
- **Integration Tests**: WebSocket connections, Repository
- **Security Tests**: Auth interceptor, XSS prevention
- **Coverage Target**: 80%+

## Integration Points

- **easyorange-user**: User info for messaging
- **easyorange-framework**: WebSocket, Security, Events
- **easyorange-common-domain**: DomainEventPublisher
