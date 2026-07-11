package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cartethyia.easyorange.common.exception.ConcurrentUpdateException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.ImmutablePersonalInfo;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepositoryImpl 测试")
class UserRepositoryImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEntityMapper entityMapper;

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
        userRepository = new UserRepositoryImpl(userMapper, entityMapper);
    }

    private UserEntity buildTestEntity() {
        return UserEntity.builder()
            .id("1")
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
            .version(0)
            .build();
    }

    private User buildTestDomainUser() {
        return User.builder()
            .id("1")
            .credentials(new Credentials("testuser", "$2a$10$encoded"))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .contactInfo(new ContactInfo("test@example.com", "13812345678"))
            .personalInfo(ImmutablePersonalInfo.builder()
                .realName("张三")
                .nickName("小张")
                .sex(Sex.MALE)
                .studentId("2024001")
                .avatar("/avatar/test.png")
                .build())
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
            when(userMapper.selectById("1")).thenReturn(entity);
            when(entityMapper.toDomain(entity)).thenReturn(buildTestDomainUser());

            Optional<User> result = userRepository.findById("1");

            assertThat(result).isPresent();
            User user = result.get();
            assertThat(user.getId()).isEqualTo("1");
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getPassword()).isEqualTo("$2a$10$encoded");
            assertThat(user.getUserType()).isEqualTo(UserType.NORMAL);
            assertThat(user.getStatus()).isEqualTo(UserStatus.NORMAL);
            assertThat(user.getContactInfo().email()).isEqualTo("test@example.com");
            assertThat(user.getContactInfo().phone()).isEqualTo("13812345678");
        }

        @Test
        @DisplayName("用户不存在时应返回 empty")
        void shouldReturnEmptyWhenNotFound() {
            when(userMapper.selectById("999")).thenReturn(null);

            Optional<User> result = userRepository.findById("999");

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
            when(entityMapper.toDomain(any(UserEntity.class))).thenReturn(buildTestDomainUser());

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
                .credentials(new Credentials("newuser", "$2a$10$encoded"))
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .build();
            when(entityMapper.from(domainUser)).thenReturn(UserEntity.builder()
                .username("newuser")
                .password("$2a$10$encoded")
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .build());
            doAnswer(invocation -> {
                UserEntity e = invocation.getArgument(0);
                e.setId("1");
                return 1;
            }).when(userMapper).insert(any(UserEntity.class));

            User result = userRepository.save(domainUser);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("1");
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
                .id("1")
                .credentials(new Credentials("testuser", "password"))
                .contactInfo(new ContactInfo("updated@example.com", null))
                .build();
            when(entityMapper.from(domainUser)).thenReturn(UserEntity.builder()
                .id("1")
                .username("testuser")
                .password("password")
                .email("updated@example.com")
                .build());
            when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

            userRepository.update(domainUser);

            verify(userMapper).updateById(any(UserEntity.class));
        }

        @Test
        @DisplayName("更新失败时应返回 false")
        void shouldReturnFalseWhenUpdateFails() {
            User domainUser = User.builder()
                .id("999")
                .credentials(new Credentials("testuser", "password"))
                .build();
            when(entityMapper.from(domainUser)).thenReturn(UserEntity.builder()
                .id("999")
                .username("testuser")
                .password("password")
                .build());
            when(userMapper.updateById(any(UserEntity.class))).thenReturn(0);

            assertThatThrownBy(() -> userRepository.update(domainUser))
                .isInstanceOf(ConcurrentUpdateException.class);
        }
    }

    @Nested
    @DisplayName("updateLoginInfo")
    class UpdateLoginInfoTests {

        @Test
        @DisplayName("应使用 update wrapper 更新登录信息")
        void shouldUpdateLoginInfoWithWrapper() {
            when(userMapper.update(isNull(), any())).thenReturn(1);

            userRepository.updateLoginInfo("1", "192.168.1.1");

            verify(userMapper).update(isNull(), any());
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteByIdTests {

        @Test
        @DisplayName("应委托给 mapper 删除")
        void shouldDelegateToMapper() {
            userRepository.deleteById("1");

            verify(userMapper).deleteById("1");
        }
    }

    @Nested
    @DisplayName("findByLoginIdentifier")
    class FindByLoginIdentifierTests {

        @Test
        @DisplayName("账号为 null 时应返回 empty")
        void shouldReturnEmptyWhenAccountIsNull() {
            Optional<User> result = userRepository.findByLoginIdentifier(null);

            assertThat(result).isEmpty();
            verify(userMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("账号为空白时应返回 empty")
        void shouldReturnEmptyWhenAccountIsBlank() {
            Optional<User> result = userRepository.findByLoginIdentifier("   ");

            assertThat(result).isEmpty();
            verify(userMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("应通过邮箱查找用户")
        void shouldFindByEmail() {
            UserEntity entity = buildTestEntity();
            when(userMapper.selectOne(any())).thenReturn(entity);
            when(entityMapper.toDomain(any(UserEntity.class))).thenReturn(buildTestDomainUser());

            Optional<User> result = userRepository.findByLoginIdentifier("test@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getContactInfo().email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("应通过手机号查找用户")
        void shouldFindByPhone() {
            UserEntity entity = buildTestEntity();
            when(userMapper.selectOne(any())).thenReturn(entity);
            when(entityMapper.toDomain(any(UserEntity.class))).thenReturn(buildTestDomainUser());

            Optional<User> result = userRepository.findByLoginIdentifier("13812345678");

            assertThat(result).isPresent();
            assertThat(result.get().getContactInfo().phone()).isEqualTo("13812345678");
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
            when(entityMapper.toDomain(any(UserEntity.class))).thenReturn(buildTestDomainUser());

            Optional<User> result = userRepository.findByPhone("13812345678");

            assertThat(result).isPresent();
            assertThat(result.get().getContactInfo().phone()).isEqualTo("13812345678");
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
            when(entityMapper.toDomain(any(UserEntity.class))).thenReturn(buildTestDomainUser());

            Optional<User> result = userRepository.findByEmail("test@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getContactInfo().email()).isEqualTo("test@example.com");
        }
    }
}
