package com.cartethyia.easyorange.adapter.inbound.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.application.dto.QaRequest;
import com.cartethyia.easyorange.ai.application.dto.QaResponse;
import com.cartethyia.easyorange.ai.application.service.AiQaService;
import com.cartethyia.easyorange.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiQaController 测试")
class AiQaControllerTest {

    @Mock
    private AiQaService qaService;

    private AiQaController controller;

    @BeforeEach
    void setUp() {
        controller = new AiQaController(qaService);
    }

    @Nested
    @DisplayName("POST /api/ai/qa")
    class AnswerQuestionTests {

        @Test
        @DisplayName("问答 — 返回有效回答")
        void answerQuestion_success() {
            var request = new QaRequest("1", "是正品吗？", "iPhone 14", "99新", "手机数码", "¥4500", "九五新", "张三");
            var expected = new QaResponse("是正品，有官方购买凭证", true);
            when(qaService.answerQuestion(any())).thenReturn(expected);

            Result<QaResponse> result = controller.answerQuestion(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().answer()).isEqualTo("是正品，有官方购买凭证");
            assertThat(result.data().confidence()).isTrue();
            verify(qaService).answerQuestion(request);
        }
    }
}
