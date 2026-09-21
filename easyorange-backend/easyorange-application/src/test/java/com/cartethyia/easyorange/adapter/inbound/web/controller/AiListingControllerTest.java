package com.cartethyia.easyorange.adapter.inbound.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.application.listing.AutoListingService;
import com.cartethyia.easyorange.ai.domain.constant.AiResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.Result;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiListingController 测试")
class AiListingControllerTest {

    @Mock
    private AutoListingService autoListingService;

    private AiListingController controller;

    @BeforeEach
    void setUp() {
        controller = new AiListingController(autoListingService);
    }

    @Nested
    @DisplayName("POST /api/ai/auto-listing")
    class AutoListingTests {

        @Test
        @DisplayName("图片分析 — 返回 AutoListingResult")
        void autoListing_success() {
            var expected = new AutoListingResult("在管 iPhone 14", "99新", new BigDecimal("4500"), "手机数码", "2", "广州");
            when(autoListingService.analyzeImages(anyList())).thenReturn(expected);

            var imageUrls = List.of("https://example.com/img1.jpg", "https://example.com/img2.jpg");
            Result<AutoListingResult> result = controller.autoListing(imageUrls);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data()).isEqualTo(expected);
            assertThat(result.data().title()).isEqualTo("在管 iPhone 14");
            verify(autoListingService).analyzeImages(imageUrls);
        }

        @Test
        @DisplayName("识别失败 — 业务异常向上抛，由全局异常处理转成 B8002（而不是 200 + null）")
        void autoListing_serviceThrows() {
            when(autoListingService.analyzeImages(anyList()))
                    .thenThrow(BusinessException.of(AiResultCode.AI_UNAVAILABLE));

            assertThatThrownBy(() -> controller.autoListing(List.of("https://example.com/img.jpg")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo("B8002");
        }
    }
}
