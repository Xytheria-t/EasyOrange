package com.cartethyia.easyorange.ai.application.report;

import com.cartethyia.easyorange.ai.domain.model.ListingAdoptionReport;
import com.cartethyia.easyorange.ai.domain.port.ListingAdoptionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 建议采纳率读取（管理端）— 使用侧效果数字，不依赖 LLM 判分；报数须带样本量。只读委托，但应用层
 * 这一跳不能省：口径与加载在 {@link ListingAdoptionPort} 实现侧，管理端 controller 只依赖应用服务。
 */
@Service
@RequiredArgsConstructor
public class ListingAdoptionAppService {

    private final ListingAdoptionPort adoptionPort;

    @Transactional(readOnly = true)
    /** 读采纳率报表（只读委托，口径在端口实现侧）。 */
    public ListingAdoptionReport report() {
        return adoptionPort.report();
    }
}
