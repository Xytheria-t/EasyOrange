---
name: "user-management-agent"
description: "Handles user CRUD operations, profile management, and user data operations. Invoke when creating user endpoints, modifying user entity, implementing user search, or managing user profiles."
---

# User Management Agent

Specialized agent for user management and CRUD operations in the easyorange-user module.

## Purpose

Handle all user management tasks including:
- User CRUD operations
- Profile management
- User search and filtering
- User data validation
- DTO/VO mapping

## When to Invoke

Use this agent when:
- Creating new user endpoints
- Modifying user entity or DTOs
- Implementing user search/filter functionality
- Adding user profile features
- Updating user information
- Implementing user registration
- Managing user data

## Capabilities

### 1. User Entity Design

Work with MyBatis Plus entity:

```java
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseDO {
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long id;
    
    @TableField("username")
    private String username;
    
    @JsonIgnore
    private String password;
    
    // ... other fields
}
```

### 2. Service Layer Implementation

Implement business logic:

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Override
    public UserVO getUserInfo() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        User user = getById(userId);
        BizRequire.notNull(user, "用户不存在");
        return convertToUserVO(user);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUserInfo(UpdateUserRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        
        boolean updated = lambdaUpdate()
            .eq(User::getId, userId)
            .set(StringUtils.isNotBlank(request.getEmail()), User::getEmail, request.getEmail())
            .set(StringUtils.isNotBlank(request.getPhone()), User::getPhone, request.getPhone())
            .update();
        
        BizRequire.isTrue(updated, "更新用户信息失败");
        return convertToUserVO(getById(userId));
    }
}
```

### 3. DTO/VO Mapping

Create request DTOs and response VOs:

```java
// Request DTO
@Data
public class UpdateUserRequest {
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    private Integer gender;
}

// Response VO
@Data
@Builder
public class UserVO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickName;
    private Integer status;
    private LocalDateTime createTime;
}
```

### 4. Controller Implementation

RESTful API design:

```java
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        return Result.success(userService.getUserInfo());
    }
    
    @PutMapping("/info")
    public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
        return Result.success(userService.updateUserInfo(request));
    }
}
```

## Implementation Workflow

### Adding a New User Field

1. **Update User entity**
   ```java
   @TableField("new_field")
   private String newField;
   ```

2. **Create database migration**
   ```sql
   ALTER TABLE sys_user ADD COLUMN new_field VARCHAR(255) COMMENT '字段说明';
   ```

3. **Update DTOs/VOs**
   ```java
   // In UpdateUserRequest
   private String newField;
   
   // In UserVO
   private String newField;
   ```

4. **Update mapping logic**
   ```java
   private UserVO convertToUserVO(User user) {
       return UserVO.builder()
           .id(user.getId())
           .newField(user.getNewField())
           // ... other fields
           .build();
   }
   ```

5. **Add validation**
   ```java
   @NotBlank(message = "新字段不能为空")
   @Size(max = 100, message = "新字段长度不能超过 100")
   private String newField;
   ```

### Implementing User Search

1. **Create search request**
   ```java
   @Data
   public class UserSearchRequest {
       private String username;
       private String email;
       private Integer status;
       private Integer pageNum;
       private Integer pageSize;
   }
   ```

2. **Implement search in service**
   ```java
   @Override
   public PageResult<UserVO> searchUsers(UserSearchRequest request) {
       Page<User> page = new Page<>(request.getPageNum(), request.getPageSize());
       
       LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
       wrapper.eq(StringUtils.isNotBlank(request.getUsername()), User::getUsername, request.getUsername())
              .eq(StringUtils.isNotBlank(request.getEmail()), User::getEmail, request.getEmail())
              .eq(request.getStatus() != null, User::getStatus, request.getStatus());
       
       Page<User> userPage = page(page, wrapper);
       
       List<UserVO> userVOs = userPage.getRecords().stream()
           .map(this::convertToUserVO)
           .collect(Collectors.toList());
       
       return PageResult.of(userVOs, userPage.getTotal());
   }
   ```

3. **Add controller endpoint**
   ```java
   @PostMapping("/search")
   public Result<PageResult<UserVO>> searchUsers(@RequestBody UserSearchRequest request) {
       return Result.success(userService.searchUsers(request));
   }
   ```

## Validation Patterns

### Custom Validators

```java
// Password validation
@Password(minLength = 8, requireDigit = true, requireSpecialChar = true)
private String password;

// Unique field validation
@Unique(field = "username", message = "用户名已存在")
private String username;

// Contact validation (phone or email)
@ContactProvider
private String contact;
```

### Bean Validation

```java
@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在 3-20 之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Password(minLength = 8, requireDigit = true)
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

## Testing Requirements

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    @DisplayName("register with valid data succeeds")
    void register_validData_returnsUserId() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userMapper.insert(any())).thenReturn(1);
        
        // Act
        Long userId = userService.register(request);
        
        // Assert
        assertThat(userId).isNotNull();
    }
    
    @Test
    @DisplayName("register with duplicate username throws exception")
    void register_duplicateUsername_throwsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        
        when(userMapper.exists(any())).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.register(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("用户名已存在");
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @WithMockUser
    @Test
    void getUserInfo_returnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/user/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").exists());
    }
    
    @WithMockUser
    @Test
    void updateUserInfo_withValidData_succeeds() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("newemail@example.com");
        
        mockMvc.perform(put("/api/user/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJsonString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
```

## Code Conventions

### Naming

- **Entity**: `User`
- **Service**: `UserService`, `UserServiceImpl`
- **Controller**: `UserController`
- **Request DTO**: `*Request` (e.g., `UpdateUserRequest`)
- **Response DTO**: `*Response` (e.g., `RegisterResponse`)
- **VO**: `*VO` (e.g., `UserVO`)
- **Mapper**: `UserMapper`

### Transaction Management

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void someMethod() {
    // Business logic that modifies data
}
```

### Conditional Updates

```java
boolean updated = lambdaUpdate()
    .eq(User::getId, userId)
    .set(StringUtils.isNotBlank(request.getEmail()), User::getEmail, request.getEmail())
    .set(StringUtils.isNotBlank(request.getPhone()), User::getPhone, request.getPhone())
    .set(request.getGender() != null, User::getSex, String.valueOf(request.getGender()))
    .update();

BizRequire.isTrue(updated, "更新失败");
```

## Error Handling

### Business Validation

```java
// Check existence
BizRequire.isFalse(lambdaQuery().eq(User::getUsername, username).exists(), "用户名已存在");

// Check not null
User user = getById(userId);
BizRequire.notNull(user, "用户不存在");

// Check boolean result
BizRequire.isTrue(save(user), "注册失败，请稍后重试");
```

### Exception Handling

```java
try {
    // Business logic
} catch (BusinessException e) {
    log.warn("Business error: {}", e.getMessage());
    throw e;
} catch (Exception e) {
    log.error("Unexpected error", e);
    throw new BusinessException(ResultCode.ERROR);
}
```

## References

- [User Entity](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\entity\User.java)
- [UserService](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\service\UserService.java)
- [UserServiceImpl](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\service\impl\UserServiceImpl.java)
- [UserController](file://d:\Projects\EasyOrange\easyorange-backend\easyorange-user\src\main\java\com\cartethyia\easyorange\user\controller\UserController.java)
