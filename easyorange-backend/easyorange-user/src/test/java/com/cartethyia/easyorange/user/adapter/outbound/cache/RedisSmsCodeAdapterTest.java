package com.cartethyia.easyorange.user.adapter.outbound.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisSmsCodeAdapterTest {

    private static final String PHONE = "13800000000";
    private static final String CODE_KEY = "eo:user:sms:code:" + PHONE;
    private static final String LIMIT_KEY = "eo:user:sms:limit:" + PHONE;
    private static final String DAILY_KEY = "eo:user:sms:daily:" + PHONE;

    @SuppressWarnings("unchecked")
    private final RedisTemplate<Object, Object> redisTemplate = mock(RedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<Object, Object> valueOps = mock(ValueOperations.class);

    private final SmsSenderPort smsSenderPort = mock(SmsSenderPort.class);
    private final RedisSmsCodeAdapter adapter = new RedisSmsCodeAdapter(redisTemplate, smsSenderPort);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOps.increment(DAILY_KEY)).thenReturn(1L);
    }

    @Test
    @DisplayName("send 成功：先投递、后落码与间隔锁，TTL 取安全常量")
    void sendStoresStateOnlyAfterDelivery() {
        assertThat(adapter.send(PHONE)).isTrue();

        InOrder ordered = inOrder(smsSenderPort, valueOps);
        ordered.verify(smsSenderPort).send(eq(PHONE), anyString());
        ordered.verify(valueOps)
                .set(
                        eq(CODE_KEY),
                        anyString(),
                        eq(UserSecurityConstant.SMS_CODE_TTL.getSeconds()),
                        eq(TimeUnit.SECONDS));
        ordered.verify(valueOps)
                .set(
                        eq(LIMIT_KEY),
                        anyString(),
                        eq(UserSecurityConstant.SMS_SEND_INTERVAL.getSeconds()),
                        eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("send 失败：异常上抛，不落码、不白锁间隔（每日配额仍计防刷）")
    void sendFailureLeavesNoLock() {
        doThrow(new IllegalStateException("provider down")).when(smsSenderPort).send(anyString(), anyString());

        assertThatThrownBy(() -> adapter.send(PHONE)).isInstanceOf(IllegalStateException.class);

        verify(valueOps, never()).set(eq(CODE_KEY), anyString(), anyLong(), any(TimeUnit.class));
        verify(valueOps, never()).set(eq(LIMIT_KEY), anyString(), anyLong(), any(TimeUnit.class));
        verify(valueOps).increment(DAILY_KEY);
    }

    @Test
    @DisplayName("发送间隔内再发：直接拒绝，不投递不加锁")
    void sendBlockedByInterval() {
        when(redisTemplate.hasKey(LIMIT_KEY)).thenReturn(true);

        assertThat(adapter.send(PHONE)).isFalse();

        verify(smsSenderPort, never()).send(anyString(), anyString());
        verify(valueOps, never()).increment(DAILY_KEY);
    }
}
