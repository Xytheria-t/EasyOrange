package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.UserProfile;
import com.cartethyia.easyorange.user.domain.shared.enums.Sex;
import com.cartethyia.easyorange.user.domain.shared.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.shared.enums.UserType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepositoryImpl 测试")
class UserRepositoryImplTest {

    @Mock
    private UserMapper userMapper;

    private UserRepositoryImpl userRepository;

    @BeforeAll
    static void initMybatisPlusCache() {
        if (TableInfoHelper.getTableInfo(UserEntity.class) == null) {
            MybatisConfiguration configuration = new MybatisConfiguration();
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
            assistant.setCurrentNamespace("com.cartethyia.easyorange.user.infrastructure.persistence.UserMapper");
            TableInfoHelper.initTableInfo(assistant, UserEntity.class);
        }
    }

    @BeforeEach
    void setUp() {
        userRepository = new UserRepositoryImpl(userMapper);
    }

    private UserEntity buildTestEntity() {
        return UserEntity.builder()
            .id(1L)
            .username("testuser")
            .password("$2a$10$encoded")
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .email("test@example.com")
            .phone("13812345678")
            .studentId("2024001")
            .realName("张三")
            .nickName("小张")
            .sex(Sex.MALE)
            .loginIp("192.168.1.1")
            .loginDate(LocalDateTime.of(2024, 1, 1, 12, 0))
            .pwdUpdateDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .avatar("/avatar/test.png")
            .remark("测试用户")
            .build();
    }

    private User buildTestDomainUser() {
        return User.builder()
            .id(1L)
            .username("testuser")
            .password("$2a$10$encoded")
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .studentId("2024001")
            .profile(new UserProfile(
                "test@example.com",
                "13812345678",
                "张三",
                "小张",
                Sex.MALE,
                "/avatar/test.png",
                "测试用户"
            ))
            .loginInfo(new LoginInfo(
                "192.168.1.1",
                LocalDateTime.of(2024, 1, 1, 12, 0),
                LocalDateTime.of(2024, 1, 1, 0, 0)
            ))
            .build();
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("应返回领域用户")
        void shouldReturnDomainUser() {
            UserEntity entity = buildTestEntity();
            when(userMapper.selectById(1L)).thenReturn(entity);

            Optional<User> result = userRepository.findById(1L);

            assertThat(result).isPresent();
            User user = result.get();
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getPassword()).isEqualTo("$2a$10$encoded");
            assertThat(user.getUserType()).isEqualTo(UserType.NORMAL);
            assertThat(user.getStatus()).isEqualTo(UserStatus.NORMAL);
            assertThat(user.getProfile().email()).isEqualTo("test@example.com");
            assertThat(user.getProfile().phone()).isEqualTo("13812345678");
        }

        @Test
        @DisplayName("用户不存在时应返回 empty")
        void shouldReturnEmptyWhenNotFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            Optional<User> result = userRepository.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUsername")
    class FindByUsernameTests {

        @Test
        @DisplayName("应返回领域用户")
        void shouldReturnDomainUser() {
            UserEntity entity = buildTestEntity();
            when(userMapper.selectOne(any())).thenReturn(entity);

            Optional<User> result = userRepository.findByUsername("testuser");

            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("用户不存在时应返回 empty")
        void shouldReturnEmptyWhenNotFound() {
            when(userMapper.selectOne(any())).thenReturn(null);

            Optional<User> result = userRepository.findByUsername("nonexistent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("应插入并返回带 id 的用户")
        void shouldInsertAndReturnWithId() {
            User domainUser = User.builder()
                .username("newuser")
                .password("$2a$10$encoded")
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .build();
            doAnswer(invocation -> {
                UserEntity e = invocation.getArgument(0);
                e.setId(1L);
                return 1;
            }).when(userMapper).insert(any(UserEntity.class));

            User result = userRepository.save(domainUser);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(userMapper).insert(any(UserEntity.class));
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("应委托给 mapper 更新")
        void shouldDelegateToMapper() {
            User domainUser = User.builder()
                .id(1L)
                .username("testuser")
                .profile(new UserProfile("updated@example.com", null, null, null, null, null, null))
                .build();
            when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

            boolean result = userRepository.update(domainUser);

            assertThat(result).isTrue();
            verify(userMapper).updateById(any(UserEntity.class));
        }

        @Test
        @DisplayName("更新失败时应返回 false")
        void shouldReturnFalseWhenUpdateFails() {
            User domainUser = User.builder()
                .id(999L)
                .username("testuser")
                .build();
            when(userMapper.updateById(any(UserEntity.class))).thenReturn(0);

            boolean result = userRepository.update(domainUser);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("updateLoginInfo")
    class UpdateLoginInfoTests {

        @Test
        @DisplayName("应使用 update wrapper 更新登录信息")
        void shouldUpdateLoginInfoWithWrapper() {
            when(userMapper.update(isNull(), any())).thenReturn(1);

            boolean result = userRepository.updateLoginInfo(1L, "192.168.1.1");

            assertThat(result).isTrue();
            verify(userMapper).update(isNull(), any());
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteByIdTests {

        @Test
        @DisplayName("应委托给 mapper 删除")
        void shouldDelegateToMapper() {
            userRepository.deleteById(1L);

            verify(userMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("findByAccount")
    class FindByAccountTests {

        @Test
        @DisplayName("账号为 null 时应返回 empty")
        void shouldReturnEmptyWhenAccountIsNull() {
            Optional<User> result = userRepository.findByAccount(null);

            assertThat(result).isEmpty();
            verify(userMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("账号为空白时应返回 empty")
        void shouldReturnEmptyWhenAccountIsBlank() {
            Optional<User> result = userRepository.findByAccount("   ");

            assertThat(result).isEmpty();
            verify(userMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("应通过邮箱查找用户")
        void shouldFindByEmail() {
            UserEntity entity = buildTestEntity();
            when(userMapper.selectOne(any())).thenReturn(entity);

            Optional<User> result = userRepository.findByAccount("test@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getProfile().email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("应通过手机号查找用户")
        void shouldFindByPhone() {
            UserEntity entity = buildTestEntity();
            when(userMapper.selectOne(any())).thenReturn(entity);

            Optional<User> result = userRepository.findByAccount("13812345678");

            assertThat(result).isPresent();
            assertThat(result.get().getProfile().phone()).isEqualTo("13812345678");
        }
    }

    @Nested
    @DisplayName("findByPhone")
    class FindByPhoneTests {

        @Test
        @DisplayName("应返回领域用户")
        void shouldReturnDomainUser() {
            UserEntity entity = buildTestEntity();
            when(userMapper.selectOne(any())).thenReturn(entity);

            Optional<User> result = userRepository.findByPhone("13812345678");

            assertThat(result).isPresent();
            assertThat(result.get().getProfile().phone()).isEqualTo("13812345678");
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmailTests {

        @Test
        @DisplayName("应返回领域用户")
        void shouldReturnDomainUser() {
            UserEntity entity = buildTestEntity();
            when(userMapper.selectOne(any())).thenReturn(entity);

            Optional<User> result = userRepository.findByEmail("test@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getProfile().email()).isEqualTo("test@example.com");
        }
    }
}
