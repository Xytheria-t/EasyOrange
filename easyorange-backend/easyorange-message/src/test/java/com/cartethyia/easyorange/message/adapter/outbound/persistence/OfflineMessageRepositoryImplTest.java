package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfflineMessageRepositoryImpl 测试")
class OfflineMessageRepositoryImplTest {

    @Mock
    private OfflineMessageMapper mapper;

    private final MessageDataMapper messageDataMapper = new MessageDataMapper() {};

    @Test
    @DisplayName("主键不存在时插入（离线落库）")
    void save_insertsWhenAbsent() {
        var repository = new OfflineMessageRepositoryImpl(mapper, messageDataMapper);
        var offline = OfflineMessage.create("of-1", "u1", "m1", "/queue/notification");
        when(mapper.selectById("of-1")).thenReturn(null);

        repository.save(offline);

        verify(mapper).insert(any(OfflineMessageDO.class));
        verify(mapper, never()).updateById(any(OfflineMessageDO.class));
    }

    @Test
    @DisplayName("主键已存在时更新（markAsPushed 重推落状态），避免重复 INSERT 撞主键")
    void save_updatesWhenPresent() {
        var repository = new OfflineMessageRepositoryImpl(mapper, messageDataMapper);
        var offline = OfflineMessage.create("of-1", "u1", "m1", "/queue/notification");
        when(mapper.selectById("of-1")).thenReturn(OfflineMessageDO.builder().id("of-1").build());

        repository.save(offline);

        verify(mapper).updateById(any(OfflineMessageDO.class));
        verify(mapper, never()).insert(any(OfflineMessageDO.class));
    }
}
