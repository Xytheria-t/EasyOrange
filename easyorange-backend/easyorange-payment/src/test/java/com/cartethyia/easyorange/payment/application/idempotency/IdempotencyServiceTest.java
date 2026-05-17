package com.cartethyia.easyorange.payment.application.idempotency;

import com.cartethyia.easyorange.payment.domain.port.output.IdempotencyKeyRepositoryPort;
import com.cartethyia.easyorange.payment.domain.valueobject.IdempotencyKey;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService 单元测试")
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepositoryPort repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Captor
    private ArgumentCaptor<IdempotencyKey> keyCaptor;

    private static final String TEST_KEY = "test-idempotency-key";
    private static final Long TEST_USER_ID = 1001L;
    private static final String TEST_REQUEST_HASH = "abc123hash";

    @Nested
    @DisplayName("process - 新请求")
    class NewRequestTests {

        @Test
        @DisplayName("新请求执行操作并保存结果")
        void process_newRequest_executesAndSaves() throws Exception {
            Object request = new Object();
            when(objectMapper.writeValueAsString(any()))
                    .thenReturn("{}")
                    .thenReturn("\"result-data\"");
            when(repository.findByKey(TEST_KEY)).thenReturn(Optional.empty());

            Optional<String> result = idempotencyService.process(TEST_KEY, TEST_USER_ID, request,
                    () -> "result-data");

            assertThat(result).contains("result-data");
            verify(repository).save(keyCaptor.capture());
            IdempotencyKey savedKey = keyCaptor.getValue();
            assertThat(savedKey.getKey()).isEqualTo(TEST_KEY);
            assertThat(savedKey.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(savedKey.getStatus()).isEqualTo(IdempotencyKey.STATUS_PENDING);
            verify(repository).updateResponse(TEST_KEY, "\"result-data\"", IdempotencyKey.STATUS_COMPLETED);
        }
    }

    @Nested
    @DisplayName("process - 重复请求")
    class DuplicateRequestTests {

        @Test
        @DisplayName("已完成的幂等键返回缓存结果")
        void process_completedKey_returnsCached() throws Exception {
            IdempotencyKey existingKey = IdempotencyKey.builder()
                    .key(TEST_KEY)
                    .userId(TEST_USER_ID)
                    .requestHash(TEST_REQUEST_HASH)
                    .status(IdempotencyKey.STATUS_COMPLETED)
                    .responseData("\"cached-result\"")
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(repository.findByKey(TEST_KEY)).thenReturn(Optional.of(existingKey));
            when(objectMapper.readValue("\"cached-result\"", String.class)).thenReturn("cached-result");

            @SuppressWarnings("unchecked")
            IdempotencyService.IdempotentOperation<String> op = mock(IdempotencyService.IdempotentOperation.class);
            when(op.getResponseType()).thenReturn(String.class);

            Optional<String> result = idempotencyService.process(TEST_KEY, TEST_USER_ID, new Object(), op);

            assertThat(result).contains("cached-result");
            verify(op, never()).execute();
            verify(repository, never()).save(any());
            verify(repository, never()).updateResponse(any(), any(), any());
        }

        @Test
        @DisplayName("用户不匹配时抛出异常")
        void process_userMismatch_throws() throws Exception {
            IdempotencyKey existingKey = IdempotencyKey.builder()
                    .key(TEST_KEY)
                    .userId(9999L)
                    .requestHash(TEST_REQUEST_HASH)
                    .status(IdempotencyKey.STATUS_PENDING)
                    .build();
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(repository.findByKey(TEST_KEY)).thenReturn(Optional.of(existingKey));

            assertThatThrownBy(() -> idempotencyService.process(TEST_KEY, TEST_USER_ID, new Object(),
                    () -> "result"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("不属于当前用户");

            verify(repository, never()).updateResponse(any(), any(), any());
        }

        @Test
        @DisplayName("待处理的幂等键执行操作并更新")
        void process_pendingKey_executesAndUpdates() throws Exception {
            IdempotencyKey existingKey = IdempotencyKey.builder()
                    .key(TEST_KEY)
                    .userId(TEST_USER_ID)
                    .requestHash(TEST_REQUEST_HASH)
                    .status(IdempotencyKey.STATUS_PENDING)
                    .build();
            when(objectMapper.writeValueAsString(any()))
                    .thenReturn("{}")
                    .thenReturn("\"result\"");
            when(repository.findByKey(TEST_KEY)).thenReturn(Optional.of(existingKey));

            Optional<String> result = idempotencyService.process(TEST_KEY, TEST_USER_ID, new Object(),
                    () -> "result");

            assertThat(result).contains("result");
            verify(repository, never()).save(any());
            verify(repository).updateResponse(TEST_KEY, "\"result\"", IdempotencyKey.STATUS_COMPLETED);
        }
    }
}
