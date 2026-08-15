package com.cartethyia.easyorange.adapter.inbound.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.dto.CopyGenerationRequest;
import com.cartethyia.easyorange.ai.dto.CopyGenerationResult;
import com.cartethyia.easyorange.ai.service.AiCopyGenerationService;
import com.cartethyia.easyorange.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiCopyController 测试")
class AiCopyControllerTest {

    @Mock
    private AiCopyGenerationService copyGenerationService;

    private AiCopyController controller;

    @BeforeEach
    void setUp() {
        controller = new AiCopyController(copyGenerationService);
    }

    @Nested
    @DisplayName("POST /api/ai/generate-copy")
    class GenerateCopyTests {

        @Test
        @DisplayName("文案生成 — 返回 CopyGenerationResult")
        void generateCopy_success() {
            var expected = new CopyGenerationResult("超值iPhone 14", "详细描述...", "standard");
            when(copyGenerationService.generateCopy(anyString(), any(), any(), any(), anyString()))
                    .thenReturn(expected);

            var request = new CopyGenerationRequest("iPhone 14", "手机数码", "2", "¥6999", "standard");
            Result<CopyGenerationResult> result = controller.generateCopy(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().title()).isEqualTo("超值iPhone 14");
            assertThat(result.data().style()).isEqualTo("standard");
            verify(copyGenerationService)
                    .generateCopy(eq("iPhone 14"), eq("手机数码"), eq("2"), eq("¥6999"), eq("standard"));
        }

        @Test
        @DisplayName("style 缺省时归一化为 standard")
        void generateCopy_nullStyleDefaultsToStandard() {
            when(copyGenerationService.generateCopy(anyString(), any(), any(), any(), eq("standard")))
                    .thenReturn(new CopyGenerationResult("标题", "描述", "standard"));

            var request = new CopyGenerationRequest("测试商品", null, null, null, null);
            Result<CopyGenerationResult> result = controller.generateCopy(request);

            assertThat(result.isSuccess()).isTrue();
            verify(copyGenerationService).generateCopy(eq("测试商品"), isNull(), isNull(), isNull(), eq("standard"));
        }

        @Test
        @DisplayName("不同风格参数 — 传递正确")
        void generateCopy_differentStyles() {
            when(copyGenerationService.generateCopy(anyString(), any(), any(), any(), anyString()))
                    .thenReturn(new CopyGenerationResult("标题", "描述", "detailed"));

            var request = new CopyGenerationRequest("商品", "分类", "1", "¥100", "detailed");
            Result<CopyGenerationResult> result = controller.generateCopy(request);

            assertThat(result.isSuccess()).isTrue();
            verify(copyGenerationService).generateCopy(eq("商品"), eq("分类"), eq("1"), eq("¥100"), eq("detailed"));
        }
    }
}
