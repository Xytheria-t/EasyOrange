package com.cartethyia.easyorange.ai.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;

/**
 * 图片 URL → Media 的 MIME 推断 —— 图片本体在供应商那边，客户端只能给一个声明，
 * 猜错类型的后果是视觉模型拒答，所以这里的每条分支都值得钉住。
 */
@DisplayName("MediaResolver MIME 推断 -> 测试")
class MediaResolverTest {

    private static MimeType firstFormatOf(String url) {
        return MediaResolver.of(List.of(url)).getFirst().getMimeType();
    }

    @Test
    @DisplayName("按 URL 后缀认 png / webp / gif / jpg，大小写不敏感")
    void extensionMapped() {
        assertThat(firstFormatOf("https://cdn.example.com/a.png")).isEqualTo(Media.Format.IMAGE_PNG);
        assertThat(firstFormatOf("https://cdn.example.com/a.WEBP")).isEqualTo(Media.Format.IMAGE_WEBP);
        assertThat(firstFormatOf("https://cdn.example.com/a.gif")).isEqualTo(Media.Format.IMAGE_GIF);
        assertThat(firstFormatOf("https://cdn.example.com/a.jpg")).isEqualTo(Media.Format.IMAGE_JPEG);
    }

    @Test
    @DisplayName("带 query 的签名 URL 仍取路径后缀，不把 query 当后缀")
    void queryStringStrippedBeforeExtension() {
        assertThat(firstFormatOf("https://cdn.example.com/a.png?sign=abc123&t=1.png"))
                .isEqualTo(Media.Format.IMAGE_PNG);
    }

    @Test
    @DisplayName("认不出来的后缀 / 无后缀 / 未知格式一律回退 JPEG")
    void unknownFallsBackToJpeg() {
        assertThat(firstFormatOf("https://cdn.example.com/a.avif")).isEqualTo(Media.Format.IMAGE_JPEG);
        assertThat(firstFormatOf("https://cdn.example.com/opaque")).isEqualTo(Media.Format.IMAGE_JPEG);
    }

    @Test
    @DisplayName("data URL 从 mime 头取类型 —— base64 内联形态没有后缀可推断")
    void dataUrlFromHeader() {
        assertThat(firstFormatOf("data:image/webp;base64,UklGRg==")).isEqualTo(Media.Format.IMAGE_WEBP);
        assertThat(firstFormatOf("data:image/gif,raw-bytes")).isEqualTo(Media.Format.IMAGE_GIF);
    }

    @Test
    @DisplayName("data URL 头部残缺（缺分隔符 / 空类型 / 非法 MIME）回退 JPEG 而不是抛异常中断识别链路")
    void malformedDataUrlFallsBackToJpeg() {
        assertThat(firstFormatOf("data:image/png")).isEqualTo(Media.Format.IMAGE_JPEG);
        assertThat(firstFormatOf("data:;base64,iVBORw0KGgo=")).isEqualTo(Media.Format.IMAGE_JPEG);
        assertThat(firstFormatOf("data:notamime;base64,iVBORw0KGgo=")).isEqualTo(Media.Format.IMAGE_JPEG);
    }
}
