---
name: "user-event-agent"
description: "Handles event-driven architecture for user events including domain events, event publishing, and event listeners. Invoke when implementing user registration events, password change events, or any async user operations."
---

# User Event Agent

Specialized agent for event-driven architecture in the easyorange-user module.

## Purpose

Handle all event-driven tasks including:
- Domain event implementation
- Event publishing with @PublishEvent
- Event listeners
- Async user operations
- Event persistence
- Event-driven notifications

## When to Invoke

Use this agent when:
- Adding new domain events (registration, password change, etc.)
- Implementing event listeners
- Creating event-driven notifications
- Implementing async user operations
- Adding event persistence
- Integrating with message queue
- Implementing event sourcing patterns

## Event Architecture

### Event Publishing Pattern

The module uses a custom event publishing mechanism with AspectJ:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublishEvent {
    String type();
    String extractor();
}
```

Usage example:

```java
@Override
@PublishEvent(type = "UserRegistered", extractor = "userRegisteredEventExtractor")
@Transactional(rollbackFor = Exception.class)
public Long register(RegisterRequest request) {
    // Business logic
    User user = saveUser(request);
    return user.getId();
}
```

### Event Extractor Pattern

Event extractors prepare event data after method execution:

```java
@Component
public class UserRegisteredEventExtractor implements EventExtractor<UserRegisteredEvent> {
    
    private User user;
    
    @Override
    public UserRegisteredEvent extract() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setUserId(user.getId());
        event.setUsername(user.getUsername());
        event.setEmail(user.getEmail());
        event.setRegistrationTime(LocalDateTime.now());
        return event;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
}
```

### Event Publisher

```java
@Component
public class UserEventPublisher {
    
    private final DomainEventPublisher domainEventPublisher;
    
    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        domainEventPublisher.publish(event);
        log.info("Published user registered event: userId={}", event.getUserId());
    }
    
    public void publishPasswordChangedEvent(PasswordChangedEvent event) {
        domainEventPublisher.publish(event);
        log.info("Published password changed event: userId={}", event.getUserId());
    }
}
```

## Implementation Workflow

### Adding a New User Event

#### Step 1: Create Event Class

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class UserEmailVerifiedEvent extends BaseDomainEvent {
    
    private Long userId;
    private String email;
    private LocalDateTime verifiedTime;
    
    @Override
    public String getEventType() {
        return "UserEmailVerified";
    }
    
    @Override
    public String getAggregateType() {
        return "USER";
    }
    
    @Override
    public String getAggregateId() {
        return String.valueOf(userId);
    }
}
```

#### Step 2: Create Event Extractor

```java
@Component
public class UserEmailVerifiedEventExtractor implements EventExtractor<UserEmailVerifiedEvent> {
    
    private Long userId;
    private String email;
    
    @Override
    public UserEmailVerifiedEvent extract() {
        UserEmailVerifiedEvent event = new UserEmailVerifiedEvent();
        event.setUserId(userId);
        event.setEmail(email);
        event.setVerifiedTime(LocalDateTime.now());
        return event;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
}
```

#### Step 3: Add @PublishEvent Annotation

```java
@Override
@PublishEvent(type = "UserEmailVerified", extractor = "userEmailVerifiedEventExtractor")
@Transactional(rollbackFor = Exception.class)
public void verifyEmail(Long userId, String token) {
    // Verification logic
    User user = getById(userId);
    BizRequire.notNull(user, "用户不存在");
    
    user.setEmailVerified(true);
    user.setEmailVerifiedTime(LocalDateTime.now());
    updateById(user);
    
    // Set extractor data
    userEmailVerifiedEventExtractor.setUserId(userId);
    userEmailVerifiedEventExtractor.setEmail(user.getEmail());
    
    log.info("Email verified successfully: userId={}", userId);
}
```

#### Step 4: Create Event Listener

```java
@Component
@RequiredArgsConstructor
public class UserEmailVerifiedEventListener {
    
    private final NotificationService notificationService;
    private final UserCacheService userCacheService;
    
    @EventListener
    @Async
    public void handleUserEmailVerifiedEvent(UserEmailVerifiedEvent event) {
        log.info("Handling user email verified event: userId={}", event.getUserId());
        
        // Send welcome email
        notificationService.sendEmail(
            event.getEmail(),
            "Email Verification Successful",
            "Welcome! Your email has been verified."
        );
        
        // Update cache
        userCacheService.invalidateUserCache(event.getUserId());
        
        // Trigger other async operations
        // ...
    }
}
```

## Event Persistence

Events are persisted for reliability:

```java
@Service
public class DomainEventPersistenceService {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Transactional
    public void persist(BaseDomainEvent event) {
        String sql = """
            INSERT INTO domain_event 
            (event_id, event_type, aggregate_type, aggregate_id, event_data, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        jdbcTemplate.update(sql,
            event.getEventId(),
            event.getEventType(),
            event.getAggregateType(),
            event.getAggregateId(),
            JsonUtils.toJsonString(event),
            LocalDateTime.now()
        );
    }
}
```

## Event Publishing Aspect

The aspect handles event publication around transactional methods:

```java
@Aspect
@Component
@RequiredArgsConstructor
public class EventPublishingAspect {
    
    private final Map<String, EventExtractor<?>> extractors;
    private final UserEventPublisher eventPublisher;
    
    @Around("@annotation(publishEvent)")
    public Object around(ProceedingJoinPoint pjp, PublishEvent publishEvent) throws Throwable {
        // Execute target method
        Object result = pjp.proceed();
        
        // Extract event data
        EventExtractor<?> extractor = extractors.get(publishEvent.extractor());
        if (extractor != null) {
            BaseDomainEvent event = extractor.extract();
            if (event != null) {
                eventPublisher.publish(event);
            }
        }
        
        return result;
    }
}
```

## Common Event Types

### User Lifecycle Events

```java
// User registered
UserRegisteredEvent

// User logged in
UserLoggedInEvent

// User logged out
UserLoggedOutEvent

// User profile updated
UserProfileUpdatedEvent

// User email verified
UserEmailVerifiedEvent

// User account deleted
UserAccountDeletedEvent

// User account locked
UserAccountLockedEvent

// User account unlocked
UserAccountUnlockedEvent
```

### Security Events

```java
// Password changed
PasswordChangedEvent

// Password reset requested
PasswordResetRequestedEvent

// Failed login attempt
FailedLoginAttemptEvent

// Account locked due to failed attempts
AccountLockedEvent

// Two-factor authentication enabled
TwoFactorAuthEnabledEvent

// Two-factor authentication disabled
TwoFactorAuthDisabledEvent
```

## Testing Events

### Unit Tests for Event Extractor

```java
@ExtendWith(MockitoExtension.class)
class UserRegisteredEventExtractorTest {
    
    @InjectMocks
    private UserRegisteredEventExtractor extractor;
    
    @Test
    void extract_withValidUser_returnsEvent() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        extractor.setUser(user);
        
        // Act
        UserRegisteredEvent event = extractor.extract();
        
        // Assert
        assertThat(event).isNotNull();
        assertThat(event.getUserId()).isEqualTo(1L);
        assertThat(event.getUsername()).isEqualTo("testuser");
        assertThat(event.getEmail()).isEqualTo("test@example.com");
    }
}
```

### Integration Tests for Event Publishing

```java
@SpringBootTest
class UserEventIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @SpyBean
    private UserEmailVerifiedEventListener emailVerifiedListener;
    
    @Test
    @Sql("/test-data.sql")
    void verifyEmail_publishesEvent() {
        // Act
        userService.verifyEmail(1L, "verification-token");
        
        // Assert - verify event was handled
        verify(emailVerifiedListener, timeout(5000))
            .handleUserEmailVerifiedEvent(any(UserEmailVerifiedEvent.class));
    }
}
```

### Event Persistence Tests

```java
@SpringBootTest
class DomainEventPersistenceTest {
    
    @Autowired
    private DomainEventPersistenceService persistenceService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    void persist_savesEventToDatabase() {
        // Arrange
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setUserId(1L);
        event.setUsername("testuser");
        
        // Act
        persistenceService.persist(event);
        
        // Assert
        String sql = "SELECT COUNT(*) FROM domain_event WHERE event_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, event.getEventId());
        assertThat(count).isEqualTo(1);
    }
}
```

## Async Event Processing

Use @Async for non-blocking event handling:

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "eventExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-");
        executor.initialize();
        return executor;
    }
}
```

Usage:

```java
@Component
public class UserEventListener {
    
    @EventListener
    @Async("eventExecutor")
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        // Async processing
        sendWelcomeEmail(event);
        updateAnalytics(event);
        initializeUserProfile(event);
    }
    
    private void sendWelcomeEmail(UserRegisteredEvent event) {
        // Send welcome email
    }
    
    private void updateAnalytics(UserRegisteredEvent event) {
        // Update analytics
    }
    
    private void initializeUserProfile(UserRegisteredEvent event) {
        // Initialize user profile
    }
}
```

## Error Handling in Events

```java
@EventListener
@Async
public void handleUserRegisteredEvent(UserRegisteredEvent event) {
    try {
        log.info("Processing user registered event: userId={}", event.getUserId());
        
        // Process event
        sendWelcomeEmail(event);
        
        log.info("Successfully processed user registered event: userId={}", event.getUserId());
    } catch (Exception e) {
        log.error("Failed to process user registered event: userId={}, error={}", 
            event.getUserId(), e.getMessage(), e);
        
        // Optionally: retry logic
        // Optionally: send to dead letter queue
        // Optionally: alert on failure
    }
}
```

## Best Practices

### Event Naming

- Use past tense: `UserRegistered`, `PasswordChanged`
- Be descriptive: `UserEmailVerified` not `EmailVerified`
- Include aggregate: `UserLoggedIn` not `LoggedIn`

### Event Data

- Include only necessary data
- Don't include sensitive data (passwords, tokens)
- Include timestamps
- Include correlation IDs for tracing

### Event Handlers

- Keep handlers focused (single responsibility)
- Use async processing for non-critical operations
- Handle exceptions gracefully
- Log important events
- Make handlers idempotent when possible

### Transaction Boundaries

- Publish events AFTER transaction commits
- Use transactional listeners if needed
- Handle transaction rollback scenarios

## References

- [UserRegisteredEvent](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\event\UserRegisteredEvent.java)
- [PasswordChangedEvent](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\event\PasswordChangedEvent.java)
- [UserEventPublisher](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\event\UserEventPublisher.java)
- [EventPublishingAspect](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\event\aspect\EventPublishingAspect.java)
- [BaseDomainEvent](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-common\src\main\java\com\cartethyia\easyorange\common\event\BaseDomainEvent.java)
