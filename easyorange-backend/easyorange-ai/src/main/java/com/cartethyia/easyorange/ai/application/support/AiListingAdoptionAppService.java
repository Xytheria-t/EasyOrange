package com.cartethyia.easyorange.ai.application.support;

import com.cartethyia.easyorange.ai.domain.model.AiListingAdoptionReport;
import com.cartethyia.easyorange.ai.domain.port.AiListingAdoptionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 建议采纳率读取（管理端）— 使用侧效果数字，不依赖 LLM 判分；报数须带样本量。
 * <p>
 * 只读委托，但应用层这一跳不能省：口径与加载在 {@link AiListingAdoptionPort} 实现侧，管理端 controller
 * 只依赖应用服务（同 {@code AiCostReportAppService} / {@code RetrievalReviewAppService}）。
 */
@Service
@RequiredArgsConstructor
public class AiListingAdoptionAppService {

    private final AiListingAdoptionPort adoptionPort;

    @Transactional(readOnly = true)
    public AiListingAdoptionReport report() {
        return adoptionPort.report();
    }
}
