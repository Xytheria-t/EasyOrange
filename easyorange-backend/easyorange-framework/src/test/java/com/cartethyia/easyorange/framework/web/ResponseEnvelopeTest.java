package com.cartethyia.easyorange.framework.web;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.config.web.ResponseAdvice;
import com.cartethyia.easyorange.framework.exception.GlobalExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/**
 * 响应封套回归测试 — 错误响应必须只在最外层有一层 {@link Result}。
 * <p>
 * 这里刻意把 {@link GlobalExceptionHandler} 与 {@link ResponseAdvice} 装进同一个 MockMvc：
 * 只导异常处理器的切片测试看到的是「未包装」的错误体，漏掉了两者的相互作用 ——
 * 异常处理器返回 {@code ResponseEntity<Result<Void>>}，advice 若按外层类型判断，会把这个
 * 错误封套再包一层 {@code Result.success(...)}，外层 code 变成 A0000/成功、真实错误码缩进 data，
 * 前端取外层 message 会把失败提示显示成「成功」（2026-09-17 实测 400/404/500 全部如此）。
 */
@DisplayName("响应封套（ResponseAdvice + GlobalExceptionHandler）")
class ResponseEnvelopeTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler(), new ResponseAdvice(new ObjectMapper()))
                .build();
    }

    @Test
    @DisplayName("业务异常 -> 400，错误码在外层（不被包成 A0000 成功）")
    void businessException_keepsErrorCodeAtTopLevel() throws Exception {
        mockMvc.perform(get("/probe/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TEST_CODE"))
                .andExpect(jsonPath("$.message").value("测试业务异常"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("非业务异常 -> 500 兜底，错误码同样在外层")
    void systemError_keepsErrorCodeAtTopLevel() throws Exception {
        mockMvc.perform(get("/probe/broken"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("C0500"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("正常响应仍被包装为统一封套（修复不误伤原职责）")
    void normalResponse_isStillWrapped() throws Exception {
        mockMvc.perform(get("/probe/plain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.value").value(42));
    }

    @Test
    @DisplayName("控制器自行返回 Result -> 原样透传，不二次包裹")
    void explicitResult_isNotWrappedTwice() throws Exception {
        mockMvc.perform(get("/probe/explicit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B9999"))
                .andExpect(jsonPath("$.message").value("自定义失败"));
    }

    @Test
    @DisplayName("Resource 二进制响应不进封套：包成 Result 会让 Resource converter CCE 恒 500（TD-018 关联）")
    void resourceResponse_isNotWrapped() throws Exception {
        mockMvc.perform(get("/probe/binary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string("binary-content"));
    }

    @RestController
    @RequestMapping("/probe")
    static class ProbeController {

        @GetMapping("/business")
        Result<Void> business() {
            throw new TestBusinessException("测试业务异常");
        }

        @GetMapping("/broken")
        Result<Void> broken() {
            throw new IllegalStateException("代码缺陷");
        }

        @GetMapping("/plain")
        Map<String, Object> plain() {
            return Map.of("value", 42);
        }

        @GetMapping("/explicit")
        Result<Void> explicit() {
            return Result.error("B9999", "自定义失败");
        }

        @GetMapping("/binary")
        ResponseEntity<Resource> binary() {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource("binary-content".getBytes(StandardCharsets.UTF_8)) {
                        @Override
                        public String getFilename() {
                            return "probe.txt";
                        }
                    });
        }
    }

    private static class TestBusinessException extends BaseBusinessException {
        TestBusinessException(String message) {
            super(message);
        }

        @Override
        protected String defaultCode() {
            return "TEST_CODE";
        }
    }
}
