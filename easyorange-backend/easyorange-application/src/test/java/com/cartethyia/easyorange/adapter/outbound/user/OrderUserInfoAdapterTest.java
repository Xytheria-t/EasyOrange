package com.cartethyia.easyorange.adapter.outbound.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.user.domain.port.UserQueryPort;
import com.cartethyia.easyorange.user.domain.port.UserQueryPort.UserInfo;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderUserInfoAdapter 测试")
class OrderUserInfoAdapterTest {

    @Mock
    private UserQueryPort userQueryPort;

    @Test
    @DisplayName("批量查询用户名，缺失 ID 不返回")
    void findUsernames_returnsUsernameMap() {
        var port = new OrderUserInfoAdapter(userQueryPort);
        when(userQueryPort.findAllByIds(any()))
                .thenReturn(List.of(
                        new UserInfo("1", "认领方小明", "小明", "avatar1"),
                        new UserInfo("2", "资产方张三", "张三", "avatar2")));

        Map<String, String> usernames = port.findUsernames(Set.of("1", "2", "999"));

        assertThat(usernames).containsEntry("1", "认领方小明").containsEntry("2", "资产方张三").hasSize(2);
    }

    @Test
    @DisplayName("空集合返回空 Map，不查用户模块")
    void findUsernames_emptyIds_returnsEmptyMap() {
        var port = new OrderUserInfoAdapter(userQueryPort);

        assertThat(port.findUsernames(Set.of())).isEmpty();
        assertThat(port.findUsernames(null)).isEmpty();
    }
}
