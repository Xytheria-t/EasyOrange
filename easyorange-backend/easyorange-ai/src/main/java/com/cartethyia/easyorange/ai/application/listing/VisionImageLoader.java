package com.cartethyia.easyorange.ai.application.listing;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.framework.config.properties.FileUploadProperties;
import com.cartethyia.easyorange.framework.file.storage.FileStoragePort;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * 视觉识别取图 → base64 data URL。
 * <p>
 * 供应商只认「公网可达 URL 或 data URL」：上传后的 {@code /api/file/…} 相对地址与
 * localhost 地址对它都不可达（DashScope 直接报 {@code InvalidParameter}）。统一在服务端
 * 把图取回内联——本地文件读盘、公网 URL 下载——供应商不再需要回源访问本服务。
 */
@RequiredArgsConstructor
@Component
public class VisionImageLoader {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            // 显式钉住不跟随重定向：跟随后，一个公网地址就能 302 到内网，绕过下面的地址校验
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final FileStoragePort fileStorage;
    private final FileUploadProperties fileUploadProperties;
    private final AiProperties aiProperties;

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
            long size = Files.size(file);
            if (size > fileUploadProperties.maxSize()) {
                throw new IllegalStateException("图片超过大小上限: " + size + " bytes (" + identifier + ")");
            }
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
        requireAllowedHost(uri, source);
        try {
            HttpRequest request =
                    HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET().build();
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() / 100 != 2) {
                    throw new IllegalStateException("图片下载失败: HTTP " + response.statusCode() + " (" + source + ")");
                }
                return encode(readCapped(body, source), downloadMime(response, uri));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("图片下载失败: " + source, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("图片下载被中断: " + source, e);
        }
    }

    /**
     * 只放行公网地址：环回 / 链路本地（云元数据 169.254.169.254）/ 私网 / CGNAT / IPv6 ULA 一律拒。
     * 白名单按主机名精确匹配，默认空。残留缺口是解析与建连之间的 DNS rebinding 窗口 —— 堵它要在
     * 连接层锁 IP，代价与收益不成比例，这里只保证「填进来的地址本身是公网」。
     */
    private void requireAllowedHost(URI uri, String source) {
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("图片来源缺少主机名: " + source);
        }
        if (aiProperties.listing().allowedImageHosts().stream().anyMatch(host::equalsIgnoreCase)) {
            return;
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("图片来源主机无法解析: " + source, e);
        }
        for (InetAddress address : addresses) {
            if (isPrivateAddress(address)) {
                throw new IllegalArgumentException("图片来源指向内网地址，已拒绝: " + source + " -> " + address.getHostAddress());
            }
        }
    }

    private static boolean isPrivateAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            // 0.0.0.0/8 与 CGNAT 100.64/10 都不在 isSiteLocalAddress 的覆盖里
            return first == 0 || (first == 100 && second >= 64 && second <= 127);
        }
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC; // IPv6 ULA fc00::/7
    }

    /** 边下边限长 —— 整包缓冲后再校验等于先让 9 × maxSize 进堆，上限形同虚设。 */
    private byte[] readCapped(InputStream body, String source) throws IOException {
        var buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = body.read(chunk)) != -1) {
            if (buffer.size() + read > fileUploadProperties.maxSize()) {
                throw new IllegalStateException("图片超过大小上限: " + source);
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    /** 响应头给了 image/* 就信响应头，否则回退按 URL 后缀推断，最后兜底 JPEG。 */
    private static String downloadMime(HttpResponse<?> response, URI uri) {
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
