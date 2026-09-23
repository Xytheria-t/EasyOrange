package com.cartethyia.easyorange.framework.file.adapter.inbound.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.framework.config.web.ResponseAdvice;
import com.cartethyia.easyorange.framework.exception.GlobalExceptionHandler;
import com.cartethyia.easyorange.framework.file.service.FileService;
import com.cartethyia.easyorange.framework.file.service.ImageProcessingService.ImageFormat;
import com.cartethyia.easyorange.framework.file.service.ImageQueryService;
import com.cartethyia.easyorange.framework.file.service.ImageQueryService.ImageQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

/**
 * 契约冒烟 — 图片 view 端点（TD-018 同类回归的守门）。
 * <p>
 * 该端点前端已不直接调用，两次潜伏 500（webp 无编码器 / Resource 被包进 Result 封套）
 * 都是因为「没有任何测试盯它」。这里把 ResponseAdvice + GlobalExceptionHandler 与真实控制器
 * 装进同一个 MockMvc（与 {@code ResponseEnvelopeTest} 同法），钉住三条契约：
 * 二进制体不进封套、304 带 ETag、文件缺失是 400 而非 500。
 */
@DisplayName("契约冒烟：文件 view 端点")
class FileControllerContractTest {

    private static final byte[] PNG_BYTES = new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private final FileService fileService = mock(FileService.class);
    private final ImageQueryService imageQueryService = mock(ImageQueryService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FileController(fileService, imageQueryService))
                .setControllerAdvice(new GlobalExceptionHandler(), new ResponseAdvice(new ObjectMapper()))
                .build();
    }

    @Test
    @DisplayName("view -> 200 原始二进制 + ETag，不被包进 Result 封套（封套会 CCE 恒 500）")
    void view_returnsBinaryOutsideEnvelope() throws Exception {
        when(imageQueryService.getForView(eq("f-1"), isNull(), isNull(), eq(ImageFormat.WEBP), eq(0.8f), isNull()))
                .thenReturn(new ImageQueryResult(pngResource(), "image/png", "\"etag-1\"", false));

        var result = mockMvc.perform(get("/api/file/f-1/view"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG_BYTES))
                .andExpect(header().string("ETag", "\"etag-1\""))
                .andReturn();

        // 二进制体的首字节在封套里表达不出来：能读回原字节，说明确实没走 Result 包装
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(PNG_BYTES);
    }

    @Test
    @DisplayName("view 命中协商缓存 -> 304 + ETag，无响应体")
    void view_notModified_returns304() throws Exception {
        // ifNoneMatch 是请求头透传，matcher 必须按值匹配（isNull 会脱靶、mock 返 null → respond NPE）
        when(imageQueryService.getForView(
                        eq("f-2"), isNull(), isNull(), eq(ImageFormat.WEBP), eq(0.8f), eq("\"etag-2\"")))
                .thenReturn(new ImageQueryResult(pngResource(), "image/png", "\"etag-2\"", true));

        mockMvc.perform(get("/api/file/f-2/view").header("If-None-Match", "\"etag-2\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", "\"etag-2\""));
    }

    @Test
    @DisplayName("文件缺失 -> 400 而非 500（TD-018 时代的恒 500 不许回来）")
    void view_missingFile_returns400() throws Exception {
        when(imageQueryService.getForView(eq("f-3"), isNull(), isNull(), eq(ImageFormat.WEBP), eq(0.8f), isNull()))
                .thenReturn(new ImageQueryResult(null, null, null, false));

        mockMvc.perform(get("/api/file/f-3/view")).andExpect(status().isBadRequest());
    }

    private static ByteArrayResource pngResource() {
        return new ByteArrayResource(PNG_BYTES) {
            @Override
            public String getFilename() {
                return "image.png";
            }
        };
    }
}
