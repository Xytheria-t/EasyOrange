package com.cartethyia.easyorange.ai.adapter.inbound.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.application.listing.AutoListingAppService;
import com.cartethyia.easyorange.ai.domain.enums.AiResultCode;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiListingController 测试")
class AiListingControllerTest {

    @Mock
    private AutoListingAppService autoListingService;

    @Mock
    private ObjectProvider<TaskExecutor> taskExecutors;

    private AiListingController controller;

    @BeforeEach
    void setUp() {
        // 取不到执行器走内联执行（与正式装配无关），单测聚焦身份归属与事件转发
        lenient().when(taskExecutors.getIfAvailable()).thenReturn(null);
        controller = new AiListingController(autoListingService, taskExecutors);
        setAuthUser("user-1");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void setAuthUser(String userId) {
        // SecurityContextUtil 显式排除 AnonymousAuthenticationToken，桩用用户名令牌（principal 约定恒为 AuthUser）
        var auth = UsernamePasswordAuthenticationToken.authenticated(new AuthUser(userId, "tester"), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("POST /api/ai/auto-listing")
    class AutoListingTests {

        @Test
        @DisplayName("图片分析 — 返回 AutoListingResult，登录身份随调用归属 trace")
        void autoListing_success() {
            var expected = new AutoListingResult("在管 iPhone 14", "99新", new BigDecimal("4500"), "手机数码", "2", "广州");
            when(autoListingService.analyzeImages(anyList(), eq("user-1"), isNull()))
                    .thenReturn(expected);

            var imageUrls = List.of("https://example.com/img1.jpg", "https://example.com/img2.jpg");
            Result<AutoListingResult> result = controller.autoListing(imageUrls);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data()).isEqualTo(expected);
            assertThat(result.data().title()).isEqualTo("在管 iPhone 14");
            verify(autoListingService).analyzeImages(imageUrls, "user-1", null);
        }

        @Test
        @DisplayName("识别失败 — 业务异常向上抛，由全局异常处理转成 B8002（而不是 200 + null）")
        void autoListing_serviceThrows() {
            when(autoListingService.analyzeImages(anyList(), any(), isNull()))
                    .thenThrow(BusinessException.of(AiResultCode.AI_UNAVAILABLE));

            assertThatThrownBy(() -> controller.autoListing(List.of("https://example.com/img.jpg")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo("B8002");
        }
    }

    @Nested
    @DisplayName("POST /api/ai/auto-listing/stream")
    class StreamTests {

        @Test
        @DisplayName("流式识别 — 登录身份带进流式线程，service 收到非空步骤 handler")
        void stream_passesHandlerAndIdentity() {
            var expected = new AutoListingResult("在管 iPhone 14", "99新", new BigDecimal("4500"), "手机数码", "2", "广州");
            when(autoListingService.analyzeImages(anyList(), eq("user-1"), any(ChatStreamHandler.class)))
                    .thenReturn(expected);

            SseEmitter emitter = controller.stream(List.of("https://example.com/img.jpg"));

            assertThat(emitter).isNotNull();
            // done / error 事件发送在无容器 SseEmitter 上会静默收尾（send 抛 IllegalStateException 被适配层吞掉），
            // 这里只验证身份与 handler 的传递；事件协议由 SSE 适配层的既有测试覆盖
            verify(autoListingService).analyzeImages(anyList(), eq("user-1"), any(ChatStreamHandler.class));
        }

        @Test
        @DisplayName("业务异常（预算尽 / 识别失败）— 不向上抛，转 error 事件静默完成流")
        void stream_businessExceptionConvertsToErrorEvent() {
            when(autoListingService.analyzeImages(anyList(), eq("user-1"), any(ChatStreamHandler.class)))
                    .thenThrow(BusinessException.of(AiResultCode.TOKEN_BUDGET_EXCEEDED));

            SseEmitter emitter = controller.stream(List.of("https://example.com/img.jpg"));

            assertThat(emitter).isNotNull();
        }
    }
}
