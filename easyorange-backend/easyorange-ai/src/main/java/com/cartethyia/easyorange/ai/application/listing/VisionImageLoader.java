package com.cartethyia.easyorange.ai.application.listing;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.framework.config.properties.FileUploadProperties;
import com.cartethyia.easyorange.framework.file.storage.FileStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * 视觉识别取图 → base64 data URL。
 * <p>
 * 供应商只认「公网可达 URL 或 data URL」：上传后的 {@code /api/file/…} 相对地址与
 * localhost 地址对它都不可达（DashScope 直接报 {@code InvalidParameter}）。统一在服务端
 * 把图取回内联——本地文件读盘、公网 URL 下载——供应商不再需要回源访问本服务。
 */
@Component
public class VisionImageLoader {

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final FileStorage fileStorage;
    private final FileUploadProperties fileUploadProperties;

    public VisionImageLoader(FileStorage fileStorage, FileUploadProperties fileUploadProperties) {
        this.fileStorage = fileStorage;
        this.fileUploadProperties = fileUploadProperties;
    }

    /** 逐张转 data URL；任一失败即抛出，由调用方降级为「识别失败」（部分缺图的识别结果不可信）。 */
    public List<String> toDataUrls(List<String> imageUrls) {
        return imageUrls.stream().map(this::toDataUrl).toList();
    }

    private String toDataUrl(String source) {
        if (source.startsWith("data:")) {
            return source;
        }
        String localPath = localPath(source);
        if (localPath != null) {
            return encode(readLocal(localPath), mimeFromPath(localPath));
        }
        return download(source);
    }

    /**
     * 命中 {@code urlPrefix}（默认 /api/file/）即视为本地上传文件 —— 相对地址与带任意 host 的
     * 绝对地址（如前端按 location.origin 绝对化后的 localhost 地址）走同一条读盘路径，不绕 HTTP。
     */
    private @Nullable String localPath(String source) {
        String path = URI.create(source).getPath();
        String prefix = fileUploadProperties.urlPrefix();
        if (path == null || !path.startsWith(prefix)) {
            return null;
        }
        return path.substring(prefix.length());
    }

    private byte[] readLocal(String identifier) {
        try {
            // getPath 内含防穿越校验（normalize + 前缀约束），非法路径直接拒绝
            Path file = fileStorage.getPath(identifier);
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException("读取本地图片失败: " + identifier, e);
        }
    }

    private String download(String source) {
        URI uri = URI.create(source);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("不支持的图片来源: " + source);
        }
        try {
            HttpRequest request =
                    HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET().build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("图片下载失败: HTTP " + response.statusCode() + " (" + source + ")");
            }
            byte[] bytes = response.body();
            if (bytes.length > CommonConstant.FILE_MAX_SIZE) {
                throw new IllegalStateException("图片超过大小上限: " + bytes.length + " bytes (" + source + ")");
            }
            return encode(bytes, downloadMime(response, uri));
        } catch (IOException e) {
            throw new UncheckedIOException("图片下载失败: " + source, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("图片下载被中断: " + source, e);
        }
    }

    /** 响应头给了 image/* 就信响应头，否则回退按 URL 后缀推断，最后兜底 JPEG。 */
    private static String downloadMime(HttpResponse<byte[]> response, URI uri) {
        String contentType = response.headers()
                .firstValue("Content-Type")
                .map(v -> v.split(";")[0].trim().toLowerCase(Locale.ROOT))
                .orElse("");
        if (contentType.startsWith("image/")) {
            return contentType;
        }
        return mimeFromPath(uri.getPath() == null ? "" : uri.getPath());
    }

    private static String mimeFromPath(String path) {
        int query = path.indexOf('?');
        int end = query >= 0 ? query : path.length();
        int dot = path.lastIndexOf('.', end - 1);
        if (dot < 0) {
            return "image/jpeg";
        }
        return switch (path.substring(dot + 1, end).toLowerCase(Locale.ROOT)) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "image/jpeg";
        };
    }

    private static String encode(byte[] bytes, String mime) {
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
