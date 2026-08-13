package com.cartethyia.easyorange.framework.audit.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.audit.mapper.AuditLogMapper;
import com.cartethyia.easyorange.framework.config.properties.AuditLogProperties;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogCleanupTask 清理任务单元测试")
class AuditLogCleanupTaskTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    private AuditLogProperties properties;
    private AuditLogCleanupTask task;

    @BeforeEach
    void setUp() {
        properties = new AuditLogProperties();
        task = new AuditLogCleanupTask(auditLogMapper, properties);
    }

    @Nested
    @DisplayName("保留期限")
    class RetentionTests {

        @Test
        @DisplayName("删除时间点为当前时间减去配置的保留天数")
        void cleanupExpiredLogs_expireDateIsNowMinusRetentionDays() {
            properties.setRetentionDays(30);
            when(auditLogMapper.deleteExpiredLogs(any())).thenReturn(0);

            task.cleanupExpiredLogs();

            var captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(auditLogMapper).deleteExpiredLogs(captor.capture());
            assertThat(captor.getValue())
                    .isAfterOrEqualTo(LocalDateTime.now().minusDays(31))
                    .isBeforeOrEqualTo(LocalDateTime.now().minusDays(29));
        }
    }

    @Nested
    @DisplayName("分批删除")
    class BatchTests {

        @Test
        @DisplayName("无过期数据时单次调用即结束")
        void cleanupExpiredLogs_whenNothingExpired_deletesOnce() {
            when(auditLogMapper.deleteExpiredLogs(any())).thenReturn(0);

            task.cleanupExpiredLogs();

            verify(auditLogMapper).deleteExpiredLogs(any());
        }

        @Test
        @DisplayName("多批删除循环直至返回 0")
        void cleanupExpiredLogs_whenMultipleBatches_loopsUntilZero() {
            when(auditLogMapper.deleteExpiredLogs(any())).thenReturn(1000, 1000, 300, 0);

            task.cleanupExpiredLogs();

            verify(auditLogMapper, times(4)).deleteExpiredLogs(any());
        }

        @Test
        @DisplayName("删除失败时吞掉异常不影响调用方")
        void cleanupExpiredLogs_whenDeleteFails_swallowsException() {
            when(auditLogMapper.deleteExpiredLogs(any())).thenThrow(new RuntimeException("db down"));

            assertThatCode(task::cleanupExpiredLogs).doesNotThrowAnyException();
        }
    }
}
