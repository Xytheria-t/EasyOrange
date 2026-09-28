package com.cartethyia.easyorange.ai.application.support;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;

/**
 * 图片 URL → Spring AI {@link Media} 的转换 — 与「怎么调模型」无关，纯 URL 形态解析，故独立成类。
 * <p>
 * MIME 靠猜是这条链路唯一的不可信环节：图片本体在供应商那边，客户端只能给一个声明。一律标 JPEG
 * 会让 PNG/WebP 被按错误类型解码（部分视觉模型直接拒答），猜不出来时回退 JPEG 是最不坏的选项 ——
 * 猜错类型会报错，标错类型只是可能影响解码质量。
 */
final class MediaResolver {

    private static final String DATA_URL_PREFIX = "data:";

    private MediaResolver() {}

    static List<Media> of(List<String> imageUrls) {
        return imageUrls.stream()
                .map(url -> Media.builder()
                        .mimeType(mimeTypeOf(url))
                        .data(URI.create(url))
                        .build())
                .toList();
    }

    private static MimeType mimeTypeOf(String url) {
        if (url.startsWith(DATA_URL_PREFIX)) {
            return dataUrlMimeType(url).orElse(Media.Format.IMAGE_JPEG);
        }
        return switch (extensionOf(url)) {
            case "png" -> Media.Format.IMAGE_PNG;
            case "webp" -> Media.Format.IMAGE_WEBP;
            case "gif" -> Media.Format.IMAGE_GIF;
            default -> Media.Format.IMAGE_JPEG;
        };
    }

    /**
     * {@code data:[<mime>][;<param>],<payload>} 的头部解析 — 服务端取图转 base64 的内联形态，
     * 对它按后缀推断无效（MIME 头本身就是唯一信息源）。
     * <p>
     * 头部可能残缺（缺逗号的分隔符、空类型、非法 MIME），一律返回 empty 走 JPEG 兜底。分隔符缺失
     * 尤其不能当异常抛：这是外部输入的解析，畸形输入的结果只能是「猜一个」，不该中断整条识别链路。
     */
    private static Optional<MimeType> dataUrlMimeType(String url) {
        int payloadStart = url.indexOf(',', DATA_URL_PREFIX.length());
        if (payloadStart < 0) {
            return Optional.empty();
        }
        String header = url.substring(DATA_URL_PREFIX.length(), payloadStart);
        int paramsStart = header.indexOf(';');
        if (paramsStart >= 0) {
            header = header.substring(0, paramsStart);
        }
        try {
            return Optional.of(MimeType.valueOf(header));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** URL 路径后缀（去 query、转小写），无后缀返回空串。 */
    private static String extensionOf(String url) {
        int query = url.indexOf('?');
        int end = query >= 0 ? query : url.length();
        int dot = url.lastIndexOf('.', end - 1);
        return dot < 0 ? "" : url.substring(dot + 1, end).toLowerCase(Locale.ROOT);
    }
}
