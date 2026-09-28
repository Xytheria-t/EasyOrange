package com.cartethyia.easyorange.ai.application.eval;

import com.cartethyia.easyorange.ai.domain.port.AiFeedbackPort;
import com.cartethyia.easyorange.ai.domain.port.GoldenSetExportPort;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI 输出反馈入库（反馈飞轮）— 观测类数据，失败只告警不阻塞主链路。落库见 {@link AiFeedbackPort}，
 * 导出逻辑见 {@link GoldenSetExportPort}。
 * <p>
 * 用户身份在这一层解析：入参是前端回传的回答与问题（不可信），登录态才是事实来源，
 * 混在 Controller 传进来只会多一条能被填错的通道。
 */
@Component
@RequiredArgsConstructor
public class AiFeedbackAppService {

    /** scope 缺省即 chat：目前只有对话有可评价的回答，其余 AI 链路（发布 / 检索）没有反馈入口。 */
    private static final String DEFAULT_SCOPE = "chat";

    private final AiFeedbackPort feedbackPort;

    public void record(
            String scope, String question, String answer, boolean helpful, String comment, String callLogId) {
        feedbackPort.record(
                scope != null ? scope : DEFAULT_SCOPE,
                question,
                answer,
                helpful,
                comment,
                callLogId,
                SecurityContextUtil.getCurrentUserId().orElse(null));
    }
}
