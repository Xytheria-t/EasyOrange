package com.cartethyia.easyorange.user.adapter.outbound.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.aggregate.UserTestFixture;
import com.cartethyia.easyorange.user.domain.port.UserQueryPort.UserInfo;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserQueryAdapter 测试")
class UserQueryAdapterTest {

    @Mock
    private UserRepository userRepository;

    private UserQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserQueryAdapter(userRepository);
    }

    @Nested
    @DisplayName("findAllByIds")
    class FindAllByIds {

        @Test
        @DisplayName("应投影为公开信息")
        void shouldProjectPublicInfo() {
            User user = UserTestFixture.normalUser();
            when(userRepository.findAllByIds(List.of(UserTestFixture.USER_ID))).thenReturn(List.of(user));

            List<UserInfo> result = adapter.findAllByIds(List.of(UserTestFixture.USER_ID));

            assertThat(result).hasSize(1);
            UserInfo info = result.get(0);
            assertThat(info.id()).isEqualTo(UserTestFixture.USER_ID);
            assertThat(info.username()).isEqualTo(UserTestFixture.USERNAME);
            assertThat(info.nickName()).isEqualTo("小张");
            assertThat(info.avatar()).isEqualTo("/avatar/test.png");
        }

        @Test
        @DisplayName("无个人资料时昵称与头像为 null")
        void shouldReturnNullsWithoutPersonalInfo() {
            User user = UserTestFixture.minimalUser();
            when(userRepository.findAllByIds(List.of(UserTestFixture.USER_ID))).thenReturn(List.of(user));

            List<UserInfo> result = adapter.findAllByIds(List.of(UserTestFixture.USER_ID));

            assertThat(result.get(0).nickName()).isNull();
            assertThat(result.get(0).avatar()).isNull();
        }

        @Test
        @DisplayName("空 ID 列表应返回空且不查询")
        void shouldReturnEmptyForEmptyIds() {
            assertThat(adapter.findAllByIds(List.of())).isEmpty();
            assertThat(adapter.findAllByIds(null)).isEmpty();
            verify(userRepository, never()).findAllByIds(anyCollection());
        }
    }

    @Nested
    @DisplayName("count")
    class Count {

        @Test
        @DisplayName("应委托仓库统计")
        void shouldDelegateCount() {
            when(userRepository.count()).thenReturn(42L);

            assertThat(adapter.count()).isEqualTo(42L);
            verify(userRepository).count();
        }
    }
}
