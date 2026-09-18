package com.cartethyia.easyorange.adapter.inbound.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.application.service.AutoListingService;
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
            var expected = new AutoListingResult(
                    "在管 iPhone 14",
                    "99新",
                    new BigDecimal("4500"),
                    "手机数码",
                    "1",
                    "2",
                    "广州",
                    List.of("手机", "数码"),
                    List.of("正面照片", "背面照片"));
            when(autoListingService.analyzeImages(anyList())).thenReturn(expected);

            var imageUrls = List.of("https://example.com/img1.jpg", "https://example.com/img2.jpg");
            Result<AutoListingResult> result = controller.autoListing(imageUrls);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data()).isEqualTo(expected);
            assertThat(result.data().title()).isEqualTo("在管 iPhone 14");
            verify(autoListingService).analyzeImages(imageUrls);
        }

        @Test
        @DisplayName("空图片列表 — 仍可正常请求")
        void autoListing_emptyImages() {
            when(autoListingService.analyzeImages(anyList()))
                    .thenReturn(new AutoListingResult(null, null, null, null, null, null, null, List.of(), List.of()));

            Result<AutoListingResult> result = controller.autoListing(List.of());

            assertThat(result.isSuccess()).isTrue();
            verify(autoListingService).analyzeImages(List.of());
        }
    }
}
