package com.cartethyia.easyorange.ai.application.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.config.properties.FileUploadProperties;
import com.cartethyia.easyorange.framework.file.storage.FileStorage;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("VisionImageLoader 测试")
class VisionImageLoaderTest {

    @TempDir
    Path uploadDir;

    private FileStorage fileStorage;
    private VisionImageLoader loader;
    private HttpServer httpServer;

    @BeforeEach
    void setUp() {
        fileStorage = mock(FileStorage.class);
        var properties = new FileUploadProperties(uploadDir.toString(), "/api/file/", 10 * 1024 * 1024, List.of());
        loader = new VisionImageLoader(fileStorage, properties);
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    private byte[] stubLocalFile(String identifier, byte[] content) throws IOException {
        var file = uploadDir.resolve(identifier);
        Files.createDirectories(file.getParent());
        Files.write(file, content);
        when(fileStorage.getPath(identifier)).thenReturn(file);
        return content;
    }

    @Test
    @DisplayName("相对 /api/file/ 地址映射上传目录读文件，转带正确 MIME 的 data URL")
    void localRelativePath_readsFromUploadDir() throws Exception {
        var content = "png-bytes".getBytes(StandardCharsets.UTF_8);
        stubLocalFile("2026/09/22/a.png", content);

        var dataUrl = loader.toDataUrls(List.of("/api/file/2026/09/22/a.png")).getFirst();

        assertThat(dataUrl).startsWith("data:image/png;base64,");
        assertThat(Base64.getDecoder().decode(dataUrl.substring("data:image/png;base64,".length())))
                .isEqualTo(content);
    }

    @Test
    @DisplayName("带 localhost host 的绝对地址同样落本地读盘，不向该 host 发起 HTTP")
    void absoluteLocalhostUrl_stillReadsLocalFile() throws Exception {
        var content = "jpg-bytes".getBytes(StandardCharsets.UTF_8);
        stubLocalFile("2026/09/22/b.jpg", content);

        var dataUrl = loader.toDataUrls(List.of("http://localhost:5173/api/file/2026/09/22/b.jpg"))
                .getFirst();

        assertThat(dataUrl).startsWith("data:image/jpeg;base64,");
        assertThat(Base64.getDecoder().decode(dataUrl.substring("data:image/jpeg;base64,".length())))
                .isEqualTo(content);
    }

    @Test
    @DisplayName("已是 data URL 的原样透传（不重复编码）")
    void existingDataUrl_passesThrough() {
        var dataUrl = "data:image/webp;base64,abcd";

        assertThat(loader.toDataUrls(List.of(dataUrl))).containsExactly(dataUrl);
    }

    @Test
    @DisplayName("公网 http(s) URL 服务端下载后转 base64（MIME 取响应头）")
    void publicUrl_downloadedAndEncoded() throws Exception {
        var content = "served-png".getBytes(StandardCharsets.UTF_8);
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/img/cat.png", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
            exchange.close();
        });
        httpServer.start();
        var url = "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/img/cat.png";

        var dataUrl = loader.toDataUrls(List.of(url)).getFirst();

        assertThat(dataUrl).startsWith("data:image/png;base64,");
        assertThat(Base64.getDecoder().decode(dataUrl.substring("data:image/png;base64,".length())))
                .isEqualTo(content);
    }

    @Test
    @DisplayName("公网下载非 2xx — 抛异常（由调用方降级为识别失败，不发残图给供应商）")
    void publicUrl_non2xx_throws() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        httpServer.start();
        var url = "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/missing";

        assertThatThrownBy(() -> loader.toDataUrls(List.of(url)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("本地文件缺失 — 抛异常（不静默跳过）")
    void localFileMissing_throws() {
        when(fileStorage.getPath("2026/09/22/gone.jpg")).thenThrow(new RuntimeException("非法文件路径"));

        assertThatThrownBy(() -> loader.toDataUrls(List.of("/api/file/2026/09/22/gone.jpg")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("非法文件路径");
    }

    @Test
    @DisplayName("不支持的来源 scheme（如 ftp:）— 拒绝")
    void unsupportedScheme_rejected() {
        assertThatThrownBy(() -> loader.toDataUrls(List.of("ftp://example.com/a.jpg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的图片来源");
    }

    @Test
    @DisplayName("多图逐张转换，顺序保持")
    void multipleImages_preserveOrder() throws Exception {
        stubLocalFile("a.jpg", "1".getBytes(StandardCharsets.UTF_8));
        stubLocalFile("b.png", "2".getBytes(StandardCharsets.UTF_8));

        List<String> dataUrls = loader.toDataUrls(List.of("/api/file/a.jpg", "/api/file/b.png"));

        assertThat(dataUrls).hasSize(2);
        assertThat(dataUrls.get(0)).startsWith("data:image/jpeg;base64,");
        assertThat(dataUrls.get(1)).startsWith("data:image/png;base64,");
    }

    @Test
    @DisplayName("本地读取 IO 失败包装为 UncheckedIOException 上抛")
    void localReadIoFailure_unchecked() throws IOException {
        var missing = uploadDir.resolve("no/such.jpg");
        when(fileStorage.getPath("no/such.jpg")).thenReturn(missing);

        assertThatThrownBy(() -> loader.toDataUrls(List.of("/api/file/no/such.jpg")))
                .isInstanceOf(UncheckedIOException.class);
    }
}
