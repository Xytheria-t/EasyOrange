package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cartethyia.easyorange.common.exception.ConcurrentUpdateException;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepositoryImpl 测试")
class UserRepositoryImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEntityMapper entityMapper;

    @Mock
    private IdGenerator idGenerator;

    private UserRepositoryImpl userRepository;

    @BeforeAll
    static void initMybatisPlusCache() {
        if (TableInfoHelper.getTableInfo(UserDO.class) == null) {
            MybatisConfiguration configuration = new MybatisConfiguration();
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
            assistant.setCurrentNamespace("com.cartethyia.easyorange.user.infrastructure.persistence.UserMapper");
            TableInfoHelper.initTableInfo(assistant, UserDO.class);
        }
    }

    @BeforeEach
    void setUp() {
        userRepository = new UserRepositoryImpl(userMapper, entityMapper, idGenerator);
    }

    private UserDO buildTestEntity() {
        return UserDO.builder()
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
                .personalInfo(PersonalInfo.builder()
                        .realName("张三")
                        .nickName("小张")
                        .sex(Sex.MALE)
                        .studentId("2024001")
                        .avatar("/avatar/test.png")
                        .build())
                .loginInfo(new LoginInfo(
                        "192.168.1.1", LocalDateTime.of(2024, 1, 1, 12, 0), LocalDateTime.of(2024, 1, 1, 0, 0)))
                .build();
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("应返回领域用户")
        void shouldReturnDomainUser() {
            UserDO entity = buildTestEntity();
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
            UserDO entity = buildTestEntity();
            when(userMapper.selectOne(any())).thenReturn(entity);
            when(entityMapper.toDomain(any(UserDO.class))).thenReturn(buildTestDomainUser());

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
        @DisplayName("新用户无 ID 时由 IdGenerator 生成并落库")
        void shouldGenerateAndAssignIdForNewUser() {
            when(idGenerator.generateId()).thenReturn("gen-id-123");
            User domainUser = User.builder()
                    .credentials(new Credentials("newuser", "$2a$10$encoded"))
                    .userType(UserType.NORMAL)
                    .status(UserStatus.NORMAL)
                    .build();
            when(entityMapper.from(domainUser))
                    .thenReturn(UserDO.builder()
                            .username("newuser")
                            .password("$2a$10$encoded")
                            .userType(UserType.NORMAL)
                            .status(UserStatus.NORMAL)
                            .build());

            User result = userRepository.save(domainUser);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("gen-id-123");
            verify(idGenerator).generateId();
            verify(userMapper).insert(argThat((UserDO entity) -> "gen-id-123".equals(entity.getId())));
        }

        @Test
        @DisplayName("已有 ID 的用户保存时保留原 ID")
        void shouldPreserveExistingId() {
            User domainUser = User.builder()
                    .id("existing-id")
                    .credentials(new Credentials("newuser", "$2a$10$encoded"))
                    .userType(UserType.NORMAL)
                    .status(UserStatus.NORMAL)
                    .build();
            when(entityMapper.from(domainUser))
                    .thenReturn(UserDO.builder()
                            .id("existing-id")
                            .username("newuser")
                            .password("$2a$10$encoded")
                            .userType(UserType.NORMAL)
                            .status(UserStatus.NORMAL)
                            .build());

            User result = userRepository.save(domainUser);

            assertThat(result.getId()).isEqualTo("existing-id");
            verify(idGenerator, never()).generateId();
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
            when(entityMapper.from(domainUser))
                    .thenReturn(UserDO.builder()
                            .id("1")
                            .username("testuser")
                            .password("password")
                            .email("updated@example.com")
                            .build());
            when(userMapper.updateById(any(UserDO.class))).thenReturn(1);

            userRepository.update(domainUser);

            verify(userMapper).updateById(any(UserDO.class));
        }

        @Test
        @DisplayName("更新失败时应返回 false")
        void shouldReturnFalseWhenUpdateFails() {
            User domainUser = User.builder()
                    .id("999")
                    .credentials(new Credentials("testuser", "password"))
                    .build();
            when(entityMapper.from(domainUser))
                    .thenReturn(UserDO.builder()
                            .id("999")
                            .username("testuser")
                            .password("password")
                            .build());
            when(userMapper.updateById(any(UserDO.class))).thenReturn(0);

            assertThatThrownBy(() -> userRepository.update(domainUser)).isInstanceOf(ConcurrentUpdateException.class);
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
        @DisplayName("应通过邮箱查找用户（username 探测未命中后继续）")
        void shouldFindByEmail() {
            UserDO entity = buildTestEntity();
            // 第一次 selectOne 为 username 探测（返回 null），第二次为 email 探测（命中）
            when(userMapper.selectOne(any())).thenReturn(null, entity);
            when(entityMapper.toDomain(any(UserDO.class))).thenReturn(buildTestDomainUser());

            Optional<User> result = userRepository.findByLoginIdentifier("test@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getContactInfo().email()).isEqualTo("test@example.com");
            verify(userMapper, times(2)).selectOne(any());
        }

        @Test
        @DisplayName("应通过手机号查找用户（username/email 探测未命中后继续）")
        void shouldFindByPhone() {
            UserDO entity = buildTestEntity();
            when(userMapper.selectOne(any())).thenReturn(null, null, entity);
            when(entityMapper.toDomain(any(UserDO.class))).thenReturn(buildTestDomainUser());

            Optional<User> result = userRepository.findByLoginIdentifier("13812345678");

            assertThat(result).isPresent();
            assertThat(result.get().getContactInfo().phone()).isEqualTo("13812345678");
            verify(userMapper, times(3)).selectOne(any());
        }

        @Test
        @DisplayName("同时命中用户名与邮箱时应优先返回用户名匹配且不再继续探测")
        void shouldPreferUsernameMatch() {
            UserDO entity = buildTestEntity();
            when(userMapper.selectOne(any())).thenReturn(entity);
            when(entityMapper.toDomain(any(UserDO.class))).thenReturn(buildTestDomainUser());

            Optional<User> result = userRepository.findByLoginIdentifier("testuser");

            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("testuser");
            verify(userMapper, times(1)).selectOne(any());
        }
    }

    @Nested
    @DisplayName("findByPhone")
    class FindByPhoneTests {

        @Test
        @DisplayName("应返回领域用户")
        void shouldReturnDomainUser() {
            UserDO entity = buildTestEntity();
            when(userMapper.selectOne(any())).thenReturn(entity);
            when(entityMapper.toDomain(any(UserDO.class))).thenReturn(buildTestDomainUser());

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
            UserDO entity = buildTestEntity();
            when(userMapper.selectOne(any())).thenReturn(entity);
            when(entityMapper.toDomain(any(UserDO.class))).thenReturn(buildTestDomainUser());

            Optional<User> result = userRepository.findByEmail("test@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getContactInfo().email()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("countByUserType")
    class CountByUserTypeTests {

        @Test
        @DisplayName("应统计指定类型的未删除用户数")
        void shouldCountActiveUsersByType() {
            when(userMapper.selectCount(any())).thenReturn(3L);

            assertThat(userRepository.countByUserType(UserType.ADMIN)).isEqualTo(3L);
            verify(userMapper).selectCount(any());
        }

        @Test
        @DisplayName("类型为 null 时返回 0 且不查询")
        void shouldReturnZeroForNullType() {
            assertThat(userRepository.countByUserType(null)).isZero();
            verify(userMapper, never()).selectCount(any());
        }
    }
}
