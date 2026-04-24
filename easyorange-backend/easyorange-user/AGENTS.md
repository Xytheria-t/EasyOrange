# easyorange-user Module Agents

Professional agent configuration for the user management module.

## Module Overview

The `easyorange-user` module handles all user-related functionality including:
- User authentication (login/logout/register)
- User profile management
- Password management (change/forgot)
- Token-based JWT authentication
- Event-driven architecture for user events

## Available Agents

### 1. **user-auth-agent**

**Purpose**: Handle authentication-related tasks

**When to use**:
- Implementing new login methods (OAuth, SSO, etc.)
- Modifying authentication flows
- Adding token refresh logic
- Implementing logout strategies
- Rate limiting or security enhancements

**Capabilities**:
- Login strategy pattern implementation
- JWT token management
- Session handling
- Authentication security best practices

**Example**:
```
"Add WeChat OAuth login support"
"Implement remember-me functionality"
"Add multi-factor authentication"
```

### 2. **user-management-agent**

**Purpose**: Handle user CRUD operations and profile management

**When to use**:
- Creating new user endpoints
- Modifying user entity or DTOs
- Implementing user search/filter
- Adding user profile features
- User data validation

**Capabilities**:
- User entity design (MyBatis Plus)
- DTO/VO mapping
- Validation rules
- Business logic implementation

**Example**:
```
"Add user avatar upload endpoint"
"Implement user search by nickname"
"Add user role assignment"
```

### 3. **user-security-agent**

**Purpose**: Handle security-sensitive user operations

**When to use**:
- Password-related changes
- Account security features
- Sensitive data handling
- Security audit logging
- Rate limiting configuration

**Capabilities**:
- BCrypt password encoding
- Security best practices (OWASP)
- Audit trail implementation
- Rate limiter configuration
- Repeat submission prevention

**Example**:
```
"Implement password strength validation"
"Add account lockout after failed attempts"
"Implement password history check"
```

### 4. **user-event-agent**

**Purpose**: Handle event-driven architecture for user events

**When to use**:
- Adding new domain events
- Implementing event listeners
- Event-driven notifications
- Async user operations

**Capabilities**:
- Domain event pattern
- Event publishing (@PublishEvent)
- Event extraction
- Event persistence

**Example**:
```
"Add event when user email is verified"
"Send welcome email on registration"
"Log user activity events"
```

### 5. **user-cache-agent**

**Purpose**: Handle user-related caching strategies

**When to use**:
- Implementing user cache
- Cache invalidation logic
- Performance optimization
- Redis integration for user data

**Capabilities**:
- Redis cache patterns
- Cache-aside strategy
- Cache invalidation
- TTL management

**Example**:
```
"Cache user profile data"
"Invalidate cache on user update"
"Add distributed session cache"
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
5. Code review with security-reviewer
   ↓
6. Test and verify
```

### Agent Selection Matrix

| Task Type | Primary Agent | Secondary Agent |
|-----------|--------------|-----------------|
| New login method | user-auth-agent | user-security-agent |
| User registration | user-management-agent | user-event-agent |
| Password change | user-security-agent | user-event-agent |
| Profile update | user-management-agent | user-cache-agent |
| Token refresh | user-auth-agent | user-cache-agent |
| Account deletion | user-management-agent | user-security-agent |
| User search | user-management-agent | user-cache-agent |
| Security audit | user-security-agent | user-event-agent |

## Architecture Patterns

### Layered Architecture

```
Controller Layer (AuthController, UserController)
    ↓
Service Layer (UserService, LoginService)
    ↓
Repository Layer (UserMapper - MyBatis Plus)
    ↓
Entity Layer (User entity)
```

### Key Patterns Used

1. **Strategy Pattern**: LoginStrategyContext for multiple login types
2. **Event-Driven**: @PublishEvent annotation with aspect-oriented publishing
3. **DTO/VO Separation**: Request DTOs and Response VOs
4. **Validation**: Custom validators (@Password, @Unique)
5. **Security**: BCrypt encoding, JWT tokens, rate limiting

## Code Conventions

### Naming Conventions

- **Controllers**: `*Controller` (e.g., `AuthController`)
- **Services**: `*Service` (e.g., `UserService`)
- **Service Implementations**: `*ServiceImpl` (e.g., `UserServiceImpl`)
- **DTOs**: `*Request`, `*Response`, `*DTO` (e.g., `LoginRequest`)
- **VOs**: `*VO` (e.g., `UserVO`)
- **Entities**: Simple names (e.g., `User`)
- **Enums**: `*Type`, `*Status`, `*ResultCode` (e.g., `UserStatus`)
- **Events**: `*Event` (e.g., `UserRegisteredEvent`)
- **Extractors**: `*EventExtractor` (e.g., `UserRegisteredEventExtractor`)

### Transaction Management

- Use `@Transactional(rollbackFor = Exception.class)` on service methods
- Event publishing happens after transaction commit (via AspectJ)
- Business exceptions throw `BusinessException` or use `BizRequire`

### Event Publishing

```java
@PublishEvent(type = "UserRegistered", extractor = "userRegisteredEventExtractor")
@Transactional(rollbackFor = Exception.class)
public Long register(RegisterRequest request) {
    // Business logic
}
```

### Validation

```java
@Password(minLength = 8, requireDigit = true, requireSpecialChar = true)
private String password;

@Unique(field = "username", message = "用户名已存在")
private String username;
```

## Security Checklist

Before any commit:
- [ ] Passwords are BCrypt encoded
- [ ] No sensitive data in logs
- [ ] Rate limiting on auth endpoints
- [ ] Repeat submission prevention
- [ ] Input validation on all endpoints
- [ ] JWT token validation
- [ ] SQL injection prevention (use parameterized queries)
- [ ] XSS prevention (sanitize user input)

## Testing Requirements

- **Unit Tests**: Service layer (80%+ coverage)
- **Integration Tests**: Controller layer with MockMvc
- **Event Tests**: Event publishing verification
- **Security Tests**: Authentication/authorization tests

## Common Tasks

### Adding a New User Field

1. Update `User` entity
2. Create database migration
3. Update DTOs/VOs if needed
4. Update `UserAssembler` if exists
5. Update validation rules
6. Add tests

### Adding a New Login Type

1. Create new `LoginType` enum value
2. Implement `LoginService` for the type
3. Register in `LoginStrategyContext`
4. Add tests
5. Update documentation

### Adding a New User Event

1. Create event class extending `BaseDomainEvent`
2. Create event extractor implementing `EventExtractor`
3. Use `@PublishEvent` on service method
4. Create event listener if needed
5. Add tests

## Integration Points

### With easyorange-common
- `BaseDO` for base entity fields
- `Result` for API responses
- `BizRequire` for business validation
- `RateLimiter`, `RepeatSubmit` annotations

### With easyorange-framework
- JWT authentication
- Token management
- Security configuration
- MyBatis Plus configuration
- Redis caching

### With easyorange-message
- User registration notifications
- Password change notifications
- System messages

## Performance Considerations

1. **Caching**: Cache frequently accessed user data
2. **Lazy Loading**: Load user details only when needed
3. **Batch Operations**: Use batch updates for bulk operations
4. **Indexing**: Ensure proper database indexes on:
   - `username` (unique)
   - `phone` (unique)
   - `email` (unique)
   - `user_id` (primary key)

## Monitoring

Key metrics to track:
- Login success/failure rate
- Registration rate
- Password reset rate
- Token refresh rate
- API response times
- Cache hit/miss rate
- Event publishing latency

## Troubleshooting

### Common Issues

**Issue**: Login fails with "Invalid credentials"
**Solution**: Check BCrypt encoder configuration and password encoding

**Issue**: Events not publishing
**Solution**: Verify @PublishEvent annotation and extractor configuration

**Issue**: Token validation fails
**Solution**: Check JWT properties and token service

**Issue**: Cache inconsistency
**Solution**: Verify cache invalidation logic on updates

## References

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [MyBatis Plus Documentation](https://baomidou.com/)
- [JWT Specification](https://jwt.io/)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
