---
name: "user-security-agent"
description: "Handles security-sensitive user operations including password management, account security, and security auditing. Invoke when implementing password features, security validations, rate limiting, or handling sensitive user data."
---

# User Security Agent

Specialized agent for security-sensitive user operations in the easyorange-user module.

## Purpose

Handle all security-related tasks including:
- Password management (change, reset, forgot)
- Account security features
- Sensitive data handling
- Security audit logging
- Rate limiting configuration
- Input validation

## When to Invoke

Use this agent when:
- Implementing password-related features
- Adding account security features (lockout, 2FA, etc.)
- Handling sensitive user data (PII)
- Implementing security audit logging
- Configuring rate limiters
- Adding repeat submission prevention
- Implementing password validation rules
- Security code review

## Security Protocols

### Password Management

#### Change Password

```java
@Override
@PublishEvent(type = "PasswordChanged", extractor = "passwordChangedEventExtractor")
@Transactional(rollbackFor = Exception.class)
public void changePassword(ChangePasswordRequest request) {
    Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
    User user = getById(userId);
    BizRequire.notNull(user, "用户不存在");
    
    // Verify old password
    BizRequire.isTrue(passwordEncoder.matches(request.getOldPassword(), user.getPassword()), 
        "旧密码错误");
    
    // Update password
    boolean updated = lambdaUpdate()
        .eq(User::getId, userId)
        .set(User::getPassword, passwordEncoder.encode(request.getNewPassword()))
        .set(User::getPwdUpdateDate, LocalDateTime.now())
        .update();
    
    BizRequire.isTrue(updated, "修改密码失败，请稍后重试");
    
    // Set event data
    passwordChangedEventExtractor.setUserId(userId);
    
    log.info("action=changePassword success userId={}", userId);
}
```

#### Forgot Password

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void forgotPassword(ForgotPasswordRequest request) {
    // Verify phone number is registered
    User user = lambdaQuery().eq(User::getPhone, request.getPhone()).one();
    BizRequire.notNull(user, "该手机号未注册");
    
    // Verify SMS code (implement verification logic)
    // verifySmsCode(request.getPhone(), request.getSmsCode());
    
    // Reset password
    boolean updated = lambdaUpdate()
        .eq(User::getId, user.getId())
        .set(User::getPassword, passwordEncoder.encode(request.getNewPassword()))
        .set(User::getPwdUpdateDate, LocalDateTime.now())
        .update();
    
    BizRequire.isTrue(updated, "重置密码失败，请稍后重试");
    log.info("action=forgotPassword success phone={}", request.getPhone());
}
```

### Password Encoding

ALWAYS use BCrypt:

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

Usage:

```java
// Encode password
String encodedPassword = passwordEncoder.encode(rawPassword);

// Verify password
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

### Password Validation

Implement strong password rules:

```java
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {
    String message() default "密码不符合要求";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    int minLength() default 8;
    boolean requireDigit() default true;
    boolean requireSpecialChar() default false;
    boolean requireUppercase() default false;
}
```

Validator implementation:

```java
public class PasswordValidator implements ConstraintValidator<Password, String> {
    
    private int minLength;
    private boolean requireDigit;
    private boolean requireSpecialChar;
    private boolean requireUppercase;
    
    @Override
    public void initialize(Password password) {
        this.minLength = password.minLength();
        this.requireDigit = password.requireDigit();
        this.requireSpecialChar = password.requireSpecialChar();
        this.requireUppercase = password.requireUppercase();
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.length() < minLength) {
            return false;
        }
        
        if (requireDigit && !containsDigit(password)) {
            return false;
        }
        
        if (requireSpecialChar && !containsSpecialChar(password)) {
            return false;
        }
        
        if (requireUppercase && !containsUppercase(password)) {
            return false;
        }
        
        return true;
    }
    
    private boolean containsDigit(String str) {
        return str.matches(".*\\d.*");
    }
    
    private boolean containsSpecialChar(String str) {
        return str.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    }
    
    private boolean containsUppercase(String str) {
        return str.matches(".*[A-Z].*");
    }
}
```

Usage in DTO:

```java
@Data
public class ChangePasswordRequest {
    
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    
    @NotBlank(message = "新密码不能为空")
    @Password(minLength = 8, requireDigit = true, requireSpecialChar = true)
    private String newPassword;
}
```

## Security Annotations

### Rate Limiting

Protect endpoints from brute force:

```java
@RateLimiter(key = "auth:login", count = 10, time = 60, limitType = LimitType.IP)
@PostMapping("/login")
public Result<LoginResponse> login(@Valid @RequestBody LoginDTO loginDTO) {
    return Result.success(loginStrategyContext.login(loginDTO));
}
```

Configuration:
- `key`: Redis key prefix
- `count`: Max requests allowed
- `time`: Time window in seconds
- `limitType`: IP, USER, or COMBINED

### Repeat Submission Prevention

Prevent duplicate form submissions:

```java
@RepeatSubmit(interval = 5000, message = "请勿重复提交登录请求")
@PostMapping("/login")
public Result<LoginResponse> login(@Valid @RequestBody LoginDTO loginDTO) {
    // Login logic
}
```

Configuration:
- `interval`: Minimum time between requests (milliseconds)
- `message`: Error message when duplicate detected

## Security Checklist

### Password Security

- [ ] Passwords encoded with BCrypt (strength 10+)
- [ ] Password history check (prevent reuse)
- [ ] Password expiration policy (90 days recommended)
- [ ] Minimum password length (8+ characters)
- [ ] Password complexity requirements
- [ ] No password in logs
- [ ] No password in error messages

### Account Security

- [ ] Account lockout after failed attempts (5 attempts)
- [ ] Lockout duration (15-30 minutes)
- [ ] CAPTCHA after failed attempts
- [ ] Session timeout (30 minutes inactivity)
- [ ] Concurrent session control
- [ ] Force logout on password change

### Data Protection

- [ ] Sensitive data encrypted at rest
- [ ] HTTPS required for transmission
- [ ] No sensitive data in logs
- [ ] Input validation on all endpoints
- [ ] Output encoding to prevent XSS
- [ ] SQL injection prevention (parameterized queries)

### Audit Logging

- [ ] Login attempts logged (success/failure)
- [ ] Password changes logged
- [ ] Profile updates logged
- [ ] Account creation logged
- [ ] Logout events logged
- [ ] Failed authorization logged

## Implementation Patterns

### Account Lockout

```java
@Service
@RequiredArgsConstructor
public class LoginSecurityService {
    
    private final RedisCache redisCache;
    
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    
    public void recordFailedAttempt(String username) {
        String key = "login:failed:" + username;
        int attempts = redisCache.increment(key);
        
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            lockAccount(username);
        }
    }
    
    public void lockAccount(String username) {
        String lockKey = "login:locked:" + username;
        redisCache.set(lockKey, "locked", LOCKOUT_DURATION_MINUTES * 60);
        log.warn("Account locked due to multiple failed attempts: username={}", username);
    }
    
    public boolean isAccountLocked(String username) {
        String lockKey = "login:locked:" + username;
        return Boolean.TRUE.equals(redisCache.get(lockKey));
    }
    
    public void resetFailedAttempts(String username) {
        String key = "login:failed:" + username;
        redisCache.delete(key);
    }
}
```

### Password History

```java
@Service
@RequiredArgsConstructor
public class PasswordHistoryService {
    
    private final RedisCache redisCache;
    private final BCryptPasswordEncoder passwordEncoder;
    
    private static final int PASSWORD_HISTORY_SIZE = 5;
    
    public boolean isPasswordInHistory(Long userId, String newPassword) {
        String key = "password:history:" + userId;
        List<String> history = redisCache.getList(key);
        
        if (history == null || history.isEmpty()) {
            return false;
        }
        
        return history.stream()
            .anyMatch(encoded -> passwordEncoder.matches(newPassword, encoded));
    }
    
    public void addToHistory(Long userId, String encodedPassword) {
        String key = "password:history:" + userId;
        
        redisCache.rightPush(key, encodedPassword);
        redisCache.leftTrim(key, 0, PASSWORD_HISTORY_SIZE - 1);
        redisCache.expire(key, 180 * 24 * 60 * 60); // 180 days
    }
}
```

### Sensitive Data Masking

```java
@Data
public class UserVO {
    
    private Long id;
    private String username;
    
    @JsonSerialize(using = PhoneMaskSerializer.class)
    private String phone;
    
    @JsonSerialize(using = EmailMaskSerializer.class)
    private String email;
    
    // Never expose password
    // @JsonIgnore
    // private String password;
}
```

Custom serializers:

```java
public class PhoneMaskSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String phone, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (phone != null && phone.length() >= 7) {
            String masked = phone.substring(0, 3) + "****" + phone.substring(7);
            gen.writeString(masked);
        } else {
            gen.writeString(phone);
        }
    }
}
```

## Security Testing

### Password Security Tests

```java
@SpringBootTest
class PasswordSecurityTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Test
    void password_isEncodedWithBCrypt() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("SecurePass123!");
        
        Long userId = userService.register(request);
        User user = userService.getById(userId);
        
        assertThat(user.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("SecurePass123!", user.getPassword())).isTrue();
    }
    
    @Test
    void changePassword_oldPasswordIncorrect_throwsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongPassword");
        request.setNewPassword("NewSecurePass123!");
        
        assertThatThrownBy(() -> userService.changePassword(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("旧密码错误");
    }
}
```

### Rate Limiting Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class RateLimiterTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void login_exceedRateLimit_returns429() throws Exception {
        // Make 10 successful requests
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"test\",\"password\":\"test123\"}"))
                .andExpect(status().isOk());
        }
        
        // 11th request should be rate limited
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"test123\"}"))
            .andExpect(status().isTooManyRequests());
    }
}
```

## Error Handling

### Secure Error Messages

```java
// GOOD: Generic error message
try {
    userService.login(username, password);
} catch (BusinessException e) {
    log.warn("Login failed: username={}", username);
    throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
}

// BAD: Reveals too much information
throw new BusinessException("用户不存在");  // Don't reveal if user exists
throw new BusinessException("密码错误，还剩 3 次尝试");  // Don't reveal attempt count
```

### Logging Security Events

```java
// Log security events (without sensitive data)
log.info("action=login_success username={}", username);
log.warn("action=login_failed username={} reason=invalid_credentials", username);
log.warn("action=account_locked username={} reason=too_many_failures", username);
log.info("action=password_changed userId={}", userId);
log.warn("action=logout userId={}", userId);

// NEVER log sensitive data
// log.error("Login failed: password={}", password);  // BAD!
// log.info("User details: {}", user);  // BAD if user contains password!
```

## References

- [ChangePasswordRequest](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\dto\request\ChangePasswordRequest.java)
- [ForgotPasswordRequest](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\dto\request\ForgotPasswordRequest.java)
- [PasswordValidator](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\validation\PasswordValidator.java)
- [RateLimiter](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-common\src\main\java\com\cartethyia\easyorange\common\annotation\RateLimiter.java)
- [RepeatSubmit](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-common\src\main\java\com\cartethyia\easyorange\common\annotation\RepeatSubmit.java)
