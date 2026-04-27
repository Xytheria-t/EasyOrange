---
name: "user-cache-agent"
description: "Handles user-related caching strategies including Redis cache implementation, cache invalidation, and performance optimization. Invoke when implementing user cache, optimizing data access, or managing distributed sessions."
---

# User Cache Agent

Specialized agent for caching strategies in the easyorange-user module.

## Purpose

Handle all caching-related tasks including:
- User data caching with Redis
- Cache invalidation strategies
- Cache-aside pattern implementation
- Distributed session management
- Performance optimization
- TTL (Time To Live) management

## When to Invoke

Use this agent when:
- Implementing user profile cache
- Adding cache invalidation logic
- Optimizing frequently accessed data
- Implementing distributed sessions
- Configuring Redis cache
- Adding cache warming strategies
- Handling cache penetration/breakdown/avalanche

## Cache Architecture

### Cache Service

```java
@Service
@RequiredArgsConstructor
public class UserCacheService {
    
    private final RedisCache redisCache;
    
    private static final String USER_CACHE_KEY_PREFIX = "user:info:";
    private static final long USER_CACHE_TTL = 30; // minutes
    
    /**
     * Get user from cache
     */
    public UserVO getUserCache(Long userId) {
        String key = USER_CACHE_KEY_PREFIX + userId;
        return redisCache.get(key, UserVO.class);
    }
    
    /**
     * Set user cache
     */
    public void setUserCache(UserVO userVO) {
        String key = USER_CACHE_KEY_PREFIX + userVO.getId();
        redisCache.set(key, userVO, USER_CACHE_TTL * 60);
    }
    
    /**
     * Delete user cache
     */
    public void deleteUserCache(Long userId) {
        String key = USER_CACHE_KEY_PREFIX + userId;
        redisCache.delete(key);
    }
    
    /**
     * Delete user cache by username
     */
    public void deleteUserCacheByUsername(String username) {
        // If you need to query userId by username first
        String userKey = "user:username:" + username;
        Long userId = redisCache.get(userKey, Long.class);
        if (userId != null) {
            deleteUserCache(userId);
        }
    }
}
```

### Cache-Aside Pattern

```java
@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {
    
    private final UserMapper userMapper;
    private final UserCacheService userCacheService;
    
    @Override
    public UserVO getUserById(Long userId) {
        // 1. Try to get from cache
        UserVO cached = userCacheService.getUserCache(userId);
        if (cached != null) {
            return cached;
        }
        
        // 2. Cache miss, query database
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        
        // 3. Convert to VO
        UserVO userVO = convertToUserVO(user);
        
        // 4. Set cache
        userCacheService.setUserCache(userVO);
        
        return userVO;
    }
}
```

## Cache Invalidation Strategies

### Write-Through Cache

Update both database and cache together:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public UserVO updateUserInfo(UpdateUserRequest request) {
    Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
    
    // Update database
    boolean updated = lambdaUpdate()
        .eq(User::getId, userId)
        .set(StringUtils.isNotBlank(request.getEmail()), User::getEmail, request.getEmail())
        .set(StringUtils.isNotBlank(request.getPhone()), User::getPhone, request.getPhone())
        .update();
    
    BizRequire.isTrue(updated, "更新用户信息失败");
    
    // Update cache
    User updatedUser = getById(userId);
    UserVO userVO = convertToUserVO(updatedUser);
    userCacheService.setUserCache(userVO);
    
    return userVO;
}
```

### Cache Invalidation on Write

Invalidate cache on updates, let next read repopulate:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void changePassword(ChangePasswordRequest request) {
    Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
    
    // Update database
    boolean updated = lambdaUpdate()
        .eq(User::getId, userId)
        .set(User::getPassword, passwordEncoder.encode(request.getNewPassword()))
        .set(User::getPwdUpdateDate, LocalDateTime.now())
        .update();
    
    BizRequire.isTrue(updated, "修改密码失败，请稍后重试");
    
    // Invalidate cache
    userCacheService.deleteUserCache(userId);
    
    log.info("Password changed and cache invalidated: userId={}", userId);
}
```

### Event-Driven Cache Invalidation

```java
@Component
@RequiredArgsConstructor
public class UserCacheEventListener {
    
    private final UserCacheService userCacheService;
    
    @EventListener
    @Async
    public void handleUserUpdatedEvent(UserProfileUpdatedEvent event) {
        log.info("Invalidating user cache: userId={}", event.getUserId());
        userCacheService.deleteUserCache(event.getUserId());
    }
    
    @EventListener
    @Async
    public void handleUserDeletedEvent(UserAccountDeletedEvent event) {
        log.info("Invalidating user cache: userId={}", event.getUserId());
        userCacheService.deleteUserCache(event.getUserId());
    }
    
    @EventListener
    @Async
    public void handlePasswordChangedEvent(PasswordChangedEvent event) {
        log.info("Invalidating user cache: userId={}", event.getUserId());
        userCacheService.deleteUserCache(event.getUserId());
    }
}
```

## Advanced Cache Patterns

### Cache Penetration Prevention

Cache null values to prevent repeated DB queries for non-existent data:

```java
@Override
public UserVO getUserById(Long userId) {
    String key = USER_CACHE_KEY_PREFIX + userId;
    
    // Try cache
    UserVO cached = redisCache.get(key, UserVO.class);
    
    // Check for null marker
    if (cached != null) {
        return isNullMarker(cached) ? null : cached;
    }
    
    // Query database
    User user = userMapper.selectById(userId);
    
    if (user == null) {
        // Cache null with short TTL to prevent penetration
        redisCache.set(key, new UserVO(), 5 * 60); // 5 minutes
        return null;
    }
    
    UserVO userVO = convertToUserVO(user);
    redisCache.set(key, userVO, USER_CACHE_TTL * 60);
    
    return userVO;
}

private boolean isNullMarker(UserVO vo) {
    return vo.getId() == null;
}
```

### Cache Breakdown Prevention

Use distributed lock for hot keys:

```java
@Override
public UserVO getUserById(Long userId) {
    String key = USER_CACHE_KEY_PREFIX + userId;
    
    // Try cache
    UserVO cached = redisCache.get(key, UserVO.class);
    if (cached != null) {
        return cached;
    }
    
    // Try to acquire distributed lock
    String lockKey = "lock:user:" + userId;
    String requestId = UUID.randomUUID().toString();
    
    boolean locked = redisCache.tryLock(lockKey, requestId, 10);
    if (locked) {
        try {
            // Double-check cache after acquiring lock
            cached = redisCache.get(key, UserVO.class);
            if (cached != null) {
                return cached;
            }
            
            // Query database and set cache
            return loadUserAndCache(userId, key);
        } finally {
            redisCache.unlock(lockKey, requestId);
        }
    } else {
        // Wait and retry
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return getUserById(userId);
    }
}

private UserVO loadUserAndCache(Long userId, String cacheKey) {
    User user = userMapper.selectById(userId);
    if (user == null) {
        return null;
    }
    
    UserVO userVO = convertToUserVO(user);
    redisCache.set(cacheKey, userVO, USER_CACHE_TTL * 60);
    return userVO;
}
```

### Cache Avalanche Prevention

Add random TTL to prevent simultaneous expiration:

```java
@Override
public void setUserCache(UserVO userVO) {
    String key = USER_CACHE_KEY_PREFIX + userVO.getId();
    
    // Add random jitter to TTL (±5 minutes)
    long baseTtl = USER_CACHE_TTL * 60;
    long jitter = ThreadLocalRandom.current().nextLong(-300, 300);
    long ttl = baseTtl + jitter;
    
    redisCache.set(key, userVO, ttl);
}
```

## Session Caching

### Distributed Session Storage

```java
@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final RedisCache redisCache;
    private final JwtUtil jwtUtil;
    
    private static final String TOKEN_PREFIX = "login:token:";
    private static final long TOKEN_TTL = 2 * 60 * 60; // 2 hours
    
    /**
     * Create token and cache in Redis
     */
    public String createToken(User user) {
        // Generate JWT token
        String token = jwtUtil.createToken(user);
        
        // Cache token with user info
        String key = TOKEN_PREFIX + token;
        TokenInfo tokenInfo = new TokenInfo(
            user.getId(),
            user.getUsername(),
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(2)
        );
        
        redisCache.set(key, tokenInfo, TOKEN_TTL);
        
        return token;
    }
    
    /**
     * Get user info from token
     */
    public AuthUser getAuthUser(String token) {
        String key = TOKEN_PREFIX + token;
        TokenInfo tokenInfo = redisCache.get(key, TokenInfo.class);
        
        if (tokenInfo == null) {
            return null;
        }
        
        return new AuthUser(
            tokenInfo.getUserId(),
            tokenInfo.getUsername()
        );
    }
    
    /**
     * Delete token (logout)
     */
    public void delToken(String token) {
        String key = TOKEN_PREFIX + token;
        redisCache.delete(key);
        log.info("Token deleted: token={}", maskToken(token));
    }
    
    /**
     * Refresh token
     */
    public String refreshToken(String token) {
        String key = TOKEN_PREFIX + token;
        TokenInfo tokenInfo = redisCache.get(key, TokenInfo.class);
        
        if (tokenInfo == null) {
            return null;
        }
        
        // Delete old token
        redisCache.delete(key);
        
        // Create new token
        User user = new User();
        user.setId(tokenInfo.getUserId());
        user.setUsername(tokenInfo.getUsername());
        
        return createToken(user);
    }
    
    private String maskToken(String token) {
        if (token.length() < 10) {
            return "***";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}
```

## Cache Configuration

### Redis Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(RedisSerializer.string()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(RedisSerializer.json()))
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withCacheConfiguration("userCache", config.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("tokenCache", config.entryTtl(Duration.ofHours(2)))
            .transactionAware()
            .build();
    }
}
```

### Cache Properties

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 1800000  # 30 minutes
      cache-null-values: false
      allow-null-values: false
      key-prefix: "cache:"
      use-key-prefix: true
```

## Testing Cache

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class UserCacheServiceTest {
    
    @Mock
    private RedisCache redisCache;
    
    @InjectMocks
    private UserCacheService userCacheService;
    
    @Test
    void getUserCache_returnsCachedUser() {
        // Arrange
        Long userId = 1L;
        UserVO expectedUser = new UserVO();
        expectedUser.setId(userId);
        expectedUser.setUsername("testuser");
        
        when(redisCache.get("user:info:" + userId, UserVO.class))
            .thenReturn(expectedUser);
        
        // Act
        UserVO actualUser = userCacheService.getUserCache(userId);
        
        // Assert
        assertThat(actualUser).isEqualTo(expectedUser);
    }
    
    @Test
    void deleteUserCache_removesFromRedis() {
        // Arrange
        Long userId = 1L;
        
        // Act
        userCacheService.deleteUserCache(userId);
        
        // Assert
        verify(redisCache).delete("user:info:" + userId);
    }
}
```

### Integration Tests

```java
@SpringBootTest
class UserCacheIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserCacheService userCacheService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Test
    @Sql("/test-data.sql")
    void getUserById_cachesResult() {
        // Clear cache first
        redisTemplate.delete("user:info:1");
        
        // First call - should query DB and cache
        UserVO user1 = userService.getUserById(1L);
        
        // Verify cache is set
        UserVO cached = userCacheService.getUserCache(1L);
        assertThat(cached).isEqualTo(user1);
        
        // Second call - should use cache
        UserVO user2 = userService.getUserById(1L);
        assertThat(user2).isEqualTo(user1);
    }
    
    @Test
    @Sql("/test-data.sql")
    void updateUserInfo_invalidatesCache() {
        // Setup cache
        UserVO user = userService.getUserById(1L);
        assertThat(userCacheService.getUserCache(1L)).isNotNull();
        
        // Update user
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("newemail@example.com");
        userService.updateUserInfo(request);
        
        // Verify cache is invalidated
        assertThat(userCacheService.getUserCache(1L)).isNull();
    }
}
```

## Cache Monitoring

### Cache Metrics

```java
@Component
public class CacheMetrics {
    
    private final RedisCache redisCache;
    private final MeterRegistry meterRegistry;
    
    @PostConstruct
    public void init() {
        Gauge.builder("cache.hits", this, CacheMetrics::getCacheHits)
            .description("Cache hit count")
            .register(meterRegistry);
        
        Gauge.builder("cache.misses", this, CacheMetrics::getCacheMisses)
            .description("Cache miss count")
            .register(meterRegistry);
        
        Gauge.builder("cache.size", this, CacheMetrics::getCacheSize)
            .description("Cache size")
            .register(meterRegistry);
    }
    
    private double getCacheHits() {
        // Implement hit counter
        return 0;
    }
    
    private double getCacheMisses() {
        // Implement miss counter
        return 0;
    }
    
    private double getCacheSize() {
        // Implement size counter
        return 0;
    }
}
```

### Cache Logging

```java
@Service
@Slf4j
public class UserCacheService {
    
    public UserVO getUserCache(Long userId) {
        String key = USER_CACHE_KEY_PREFIX + userId;
        UserVO cached = redisCache.get(key, UserVO.class);
        
        if (cached != null) {
            log.debug("Cache hit: key={}", key);
            return cached;
        }
        
        log.debug("Cache miss: key={}", key);
        return null;
    }
    
    public void setUserCache(UserVO userVO) {
        String key = USER_CACHE_KEY_PREFIX + userVO.getId();
        redisCache.set(key, userVO, USER_CACHE_TTL * 60);
        log.debug("Cache set: key={}, ttl={}s", key, USER_CACHE_TTL * 60);
    }
}
```

## Best Practices

### Do's

- Use cache for frequently accessed, rarely changed data
- Set appropriate TTL based on data freshness requirements
- Invalidate cache on data modifications
- Use distributed locks for hot keys
- Add jitter to TTL to prevent avalanche
- Monitor cache hit/miss rates
- Use meaningful cache key prefixes

### Don'ts

- Don't cache large objects without compression
- Don't cache sensitive data without encryption
- Don't rely solely on cache for critical data
- Don't use very long TTLs for frequently changing data
- Don't cache without invalidation strategy
- Don't ignore cache failures (graceful degradation)

## Troubleshooting

### Cache Penetration

**Symptom**: High database load, many queries for non-existent keys

**Solution**: Cache null values with short TTL, use Bloom filter

### Cache Breakdown

**Symptom**: Database spike when hot key expires

**Solution**: Use distributed lock, rebuild cache before expiration

### Cache Avalanche

**Symptom**: Database overload when many keys expire simultaneously

**Solution**: Add random jitter to TTL, use staggered expiration

### Cache Inconsistency

**Symptom**: Cache and database have different data

**Solution**: Use consistent invalidation strategy, consider eventual consistency

## References

- [UserCacheService](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\application\cache\UserCacheService.java)
- [RedisConfig](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-framework\src\main\java\com\cartethyia\easyorange\framework\config\RedisConfig.java)
- [TokenService](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-framework\src\main\java\com\cartethyia\easyorange\framework\service\TokenService.java)
- [RedisCache](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-framework\src\main\java\com\cartethyia\easyorange\framework\redis\RedisCache.java)
