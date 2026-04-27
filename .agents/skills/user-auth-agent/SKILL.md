---
name: "user-auth-agent"
description: "Handles user authentication tasks including login, logout, token management, and auth security. Invoke when implementing login methods, token refresh, session management, or authentication features."
---

# User Authentication Agent

Specialized agent for user authentication-related development in the easyorange-user module.

## Purpose

Handle all authentication-related tasks including:
- Login/logout implementation
- Token management (JWT)
- Session handling
- Authentication security
- Rate limiting for auth endpoints

## When to Invoke

Use this agent when:
- Implementing new login methods (OAuth, SSO, social login)
- Modifying authentication flows
- Adding token refresh logic
- Implementing logout strategies
- Adding rate limiting to auth endpoints
- Implementing remember-me functionality
- Adding multi-factor authentication
- Fixing authentication bugs

## Capabilities

### 1. Login Strategy Pattern

Implement new login strategies following the existing pattern:

```java
@Service
@RequiredArgsConstructor
public class WebLoginServiceImpl implements LoginService {
    @Override
    public LoginResponse login(LoginDTO loginDTO) {
        // Authentication logic
        // Generate JWT token
        // Return LoginResponse
    }
}
```

### 2. JWT Token Management

Work with existing token service:

```java
private final TokenService tokenService;
private final JwtProperties jwtProperties;

// Generate token
String token = tokenService.createToken(user);

// Refresh token
String newToken = tokenService.refreshToken(oldToken);

// Delete token (logout)
tokenService.delToken(token);
```

### 3. Security Features

Implement authentication security:

- **Rate Limiting**: Use `@RateLimiter` annotation
- **Repeat Submission Prevention**: Use `@RepeatSubmit` annotation
- **Password Validation**: Use BCrypt encoding
- **Input Validation**: Use Bean Validation

### 4. Auth Controller Patterns

Follow existing controller structure:

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final LoginStrategyContext loginStrategyContext;
    private final TokenService tokenService;
    
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(loginStrategyContext.login(loginDTO));
    }
    
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        // Logout logic
    }
    
    @PostMapping("/refresh")
    public Result<String> refreshToken(@RequestHeader("Authorization") String authHeader) {
        // Refresh logic
    }
}
```

## Implementation Workflow

### Adding a New Login Type

1. **Create LoginType enum value**
   ```java
   public enum LoginType {
       WEB,
       WECHAT,
       GOOGLE,
       // New type
   }
   ```

2. **Implement LoginService**
   ```java
   @Service
   public class WeChatLoginServiceImpl implements LoginService {
       @Override
       public LoginResponse login(LoginDTO loginDTO) {
           // WeChat OAuth logic
       }
   }
   ```

3. **Register in LoginStrategyContext**
   ```java
   private final Map<String, LoginService> loginServiceMap;
   
   public LoginService getLoginService(String loginType) {
       return loginServiceMap.get(loginType);
   }
   ```

4. **Add tests**
   - Unit tests for service
   - Integration tests for controller
   - Security tests

### Implementing Token Refresh

1. **Add endpoint to AuthController**
   ```java
   @PostMapping("/refresh")
   public Result<String> refreshToken(@RequestHeader("Authorization") String authHeader) {
       String token = extractToken(authHeader);
       String newToken = tokenService.refreshToken(token);
       return Result.success(newToken);
   }
   ```

2. **Add rate limiting**
   ```java
   @RateLimiter(key = "auth:refresh", count = 20, time = 60, limitType = LimitType.IP)
   ```

3. **Add repeat submission prevention**
   ```java
   @RepeatSubmit(interval = 5000, message = "请勿重复刷新令牌")
   ```

## Security Checklist

Before completing authentication tasks:

- [ ] Rate limiting configured on all auth endpoints
- [ ] Repeat submission prevention enabled
- [ ] Password encoded with BCrypt
- [ ] JWT token validation implemented
- [ ] Token expiration configured correctly
- [ ] Logout invalidates tokens properly
- [ ] No sensitive data in logs
- [ ] Input validation on all endpoints
- [ ] HTTPS required for token transmission
- [ ] CORS configured correctly

## Testing Requirements

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    
    @Mock
    private LoginStrategyContext loginStrategyContext;
    
    @Mock
    private TokenService tokenService;
    
    @InjectMocks
    private AuthController authController;
    
    @Test
    void login_success_returnsToken() {
        // Test login success
    }
    
    @Test
    void login_invalidCredentials_returnsError() {
        // Test invalid credentials
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"test123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code\").value(200));
    }
}
```

## Common Patterns

### Token Extraction

```java
private String extractToken(String authHeader) {
    BizRequire.notBlank(authHeader, ResultCode.UNAUTHORIZED);
    BizRequire.isTrue(authHeader.startsWith(jwtProperties.getTokenPrefix()), ResultCode.UNAUTHORIZED);
    return authHeader.substring(jwtProperties.getTokenPrefix().length());
}
```

### Login Response Format

```java
@Data
@Builder
public class LoginResponse {
    private String token;
    private String tokenPrefix;
    private Long expiresIn;
    private UserInfoVO userInfo;
}
```

## Error Handling

Use consistent error handling:

```java
try {
    // Authentication logic
} catch (BadCredentialsException e) {
    log.warn("Login failed: username={}, error={}", username, e.getMessage());
    throw new BusinessException(ResultCode.UNAUTHORIZED);
} catch (Exception e) {
    log.error("Login failed: username={}", username, e);
    throw new BusinessException(ResultCode.ERROR);
}
```

## References

- [AuthController](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\controller\AuthController.java)
- [LoginStrategyContext](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\service\strategy\LoginStrategyContext.java)
- [TokenService](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-framework\src\main\java\com\cartethyia\easyorange\framework\service\TokenService.java)
- [JwtProperties](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-framework\src\main\java\com\cartethyia\easyorange\framework\config\JwtProperties.java)
