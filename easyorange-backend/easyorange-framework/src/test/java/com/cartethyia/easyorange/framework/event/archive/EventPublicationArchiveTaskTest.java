package com.cartethyia.easyorange.framework.event.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.config.properties.EventArchiveProperties;
import com.cartethyia.easyorange.framework.testsupport.PropertyBindings;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventPublicationArchiveTask 事件归档任务单元测试")
class EventPublicationArchiveTaskTest {

    private static final String INSERT_SQL =
            "INSERT INTO EVENT_PUBLICATION_ARCHIVE (ID, LISTENER_ID, EVENT_TYPE, SERIALIZED_EVENT, "
                    + "PUBLICATION_DATE, COMPLETION_DATE, STATUS, COMPLETION_ATTEMPTS, LAST_RESUBMISSION_DATE) "
                    + "SELECT ID, LISTENER_ID, EVENT_TYPE, SERIALIZED_EVENT, PUBLICATION_DATE, "
                    + "COMPLETION_DATE, STATUS, COMPLETION_ATTEMPTS, LAST_RESUBMISSION_DATE "
                    + "FROM EVENT_PUBLICATION WHERE STATUS = 'COMPLETED' AND COMPLETION_DATE < ?";

    private static final String DELETE_SQL =
            "DELETE FROM EVENT_PUBLICATION WHERE STATUS = 'COMPLETED' AND COMPLETION_DATE < ?";

    @Mock
    private JdbcTemplate jdbcTemplate;

    private EventPublicationArchiveTask task;

    @BeforeEach
    void setUp() {
        task = new EventPublicationArchiveTask(jdbcTemplate, PropertyBindings.bind(EventArchiveProperties.class));
    }

    @Test
    @DisplayName("超期行先归档再删源：INSERT SELECT 与 DELETE 同 WHERE 同 cutoff")
    void archiveCompletedEvents_movesThenDeletesWithSameCutoff() {
        when(jdbcTemplate.update(contains("INSERT INTO EVENT_PUBLICATION_ARCHIVE"), any(LocalDateTime.class)))
                .thenReturn(5);
        when(jdbcTemplate.update(contains("DELETE FROM EVENT_PUBLICATION"), any(LocalDateTime.class)))
                .thenReturn(5);

        task.archiveCompletedEvents();

        var cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(jdbcTemplate).update(eq(INSERT_SQL), cutoffCaptor.capture());
        verify(jdbcTemplate).update(eq(DELETE_SQL), cutoffCaptor.capture());

        var insertCutoff = cutoffCaptor.getAllValues().get(0);
        var deleteCutoff = cutoffCaptor.getAllValues().get(1);
        assertThat(deleteCutoff).as("DELETE 与 INSERT 同 cutoff").isEqualTo(insertCutoff);
        var expected = LocalDateTime.now().minusDays(7);
        assertThat(Math.abs(ChronoUnit.SECONDS.between(insertCutoff, expected)))
                .as("cutoff = now - archive-after-days(7)")
                .isLessThan(5);
    }

    @Test
    @DisplayName("无超期行时只执行 INSERT SELECT，不发 DELETE")
    void archiveCompletedEvents_skipsDeleteWhenNothingMoved() {
        when(jdbcTemplate.update(contains("INSERT INTO EVENT_PUBLICATION_ARCHIVE"), any(LocalDateTime.class)))
                .thenReturn(0);

        task.archiveCompletedEvents();

        verify(jdbcTemplate).update(eq(INSERT_SQL), any(LocalDateTime.class));
        verify(jdbcTemplate, never()).update(eq(DELETE_SQL), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("archive-after-days 覆写生效")
    void archiveCompletedEvents_honorsConfiguredRetention() {
        task = new EventPublicationArchiveTask(
                jdbcTemplate, PropertyBindings.bind(EventArchiveProperties.class, "archive-after-days", "30"));
        when(jdbcTemplate.update(contains("INSERT INTO EVENT_PUBLICATION_ARCHIVE"), any(LocalDateTime.class)))
                .thenReturn(1);
        when(jdbcTemplate.update(contains("DELETE FROM EVENT_PUBLICATION"), any(LocalDateTime.class)))
                .thenReturn(1);

        task.archiveCompletedEvents();

        var cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(jdbcTemplate, times(1)).update(eq(INSERT_SQL), cutoffCaptor.capture());
        var expected = LocalDateTime.now().minusDays(30);
        assertThat(Math.abs(ChronoUnit.SECONDS.between(cutoffCaptor.getValue(), expected)))
                .isLessThan(5);
    }
}
