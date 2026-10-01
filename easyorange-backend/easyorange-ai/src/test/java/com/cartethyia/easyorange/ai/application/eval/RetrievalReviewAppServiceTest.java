package com.cartethyia.easyorange.ai.application.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.domain.enums.RetrievalEvalLine;
import com.cartethyia.easyorange.ai.domain.port.RetrievalMetricQueryPort;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 检索质量回看服务测试 — 钉住三处回看特有的口径：空表不编数字、limit 收敛、空 runId 不列全量明细。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("检索质量回看服务 -> 测试")
class RetrievalReviewAppServiceTest {

    @Mock
    private RetrievalMetricQueryPort queryPort;

    @InjectMocks
    private RetrievalReviewAppService service;

    @Test
    @DisplayName("最近批次：条数上限收敛到默认上限，端口不被超量调用")
    void recentRuns_capsLimit() {
        when(queryPort.recentRuns(any(), anyInt())).thenReturn(List.of());

        service.recentRuns(RetrievalEvalLine.KNOWLEDGE, 9999);

        verify(queryPort).recentRuns(RetrievalEvalLine.KNOWLEDGE, RetrievalReviewAppService.DEFAULT_RECENT_LIMIT);
    }

    @Test
    @DisplayName("最近批次：limit 小于 1 时收敛到 1，不把 0 透给 LIMIT")
    void recentRuns_clampsNonPositiveLimit() {
        when(queryPort.recentRuns(any(), anyInt())).thenReturn(List.of());

        service.recentRuns(RetrievalEvalLine.KNOWLEDGE, 0);

        verify(queryPort).recentRuns(RetrievalEvalLine.KNOWLEDGE, 1);
    }

    @Test
    @DisplayName("下钻：runId 为空返回空页且不查库（全量明细跨批次混排没有可比基准）")
    void pageCases_blankRunIdReturnsEmptyWithoutQuery() {
        var page = service.pageCases("  ", 2, 10);

        assertThat(page.records()).isEmpty();
        assertThat(page.total()).isZero();
        verify(queryPort, never()).pageCases(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("下钻：pageSize 超上限收敛、pageNum 归一到 1 起")
    void pageCases_clampsPagingParams() {
        when(queryPort.pageCases(anyString(), anyInt(), anyInt())).thenReturn(PageResult.empty(1, 100));

        service.pageCases("run-1", 0, 5000);

        verify(queryPort).pageCases("run-1", 1, RetrievalReviewAppService.MAX_PAGE_SIZE);
    }
}
