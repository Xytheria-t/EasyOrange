package com.cartethyia.easyorange.ai.adapter;

import com.cartethyia.easyorange.ai.adapter.dto.QwenVlRequest;
import com.cartethyia.easyorange.ai.adapter.dto.QwenVlResponse;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.port.VisionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class QwenVlVisionAdapter implements VisionPort {

    private final RestClient qwenVlRestClient;
    private final AiProperties aiProperties;

    private final HttpClient imageDownloadClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String analyzeImage(String imageUrl, String prompt) {
        return analyzeImages(List.of(imageUrl), prompt);
    }

    @Override
    public String analyzeImages(List<String> imageUrls, String prompt) {
        var contents = new java.util.ArrayList<QwenVlRequest.Content>();
        for (String url : imageUrls) {
            String imageDataUri = resolveImage(url);
            contents.add(new QwenVlRequest.Content("image", null, imageDataUri));
        }
        contents.add(new QwenVlRequest.Content("text", prompt));

        var message = new QwenVlRequest.Message("user", contents);
        var input = new QwenVlRequest.Input(List.of(message));
        var request = new QwenVlRequest(aiProperties.getQwenVl().getModel(), input);

        var params = new QwenVlRequest.Parameters();
        request.setParameters(params);

        var response = qwenVlRestClient.post()
                .uri("/services/aigc/multimodal-generation/generation")
                .body(request)
                .retrieve()
                .body(QwenVlResponse.class);

        String content = response != null ? response.getFirstChoiceContent() : null;
        log.info("QwenVL analyzed images: count={}, resultLength={}", imageUrls.size(),
                content != null ? content.length() : 0);
        return content;
    }

    /**
     * Resolve an image URL to a base64 data URI.
     * Downloads the image via HTTP and encodes it as a data URI,
     * making it accessible to external AI APIs regardless of URL scheme.
     */
    String resolveImage(String imageUrl) {
        // Already a data URI — pass through
        if (imageUrl.startsWith("data:")) {
            return imageUrl;
        }

        // Build absolute URL if relative
        String absoluteUrl = imageUrl;
        if (imageUrl.startsWith("/")) {
            // If relative, try to reconstruct with request origin
            // Absolute URL is required for external download
            String baseUrl = aiProperties.getQwenVl().getBaseUrl();
            try {
                URI baseUri = URI.create(baseUrl);
                absoluteUrl = baseUri.getScheme() + "://" + baseUri.getHost() +
                        (baseUri.getPort() > 0 ? ":" + baseUri.getPort() : "") + imageUrl;
                log.warn("Relative image URL detected ({}), constructed fallback URL: {}. " +
                        "Consider configuring a public base URL for file access.", imageUrl, absoluteUrl);
            } catch (Exception e) {
                log.error("Cannot construct absolute URL from relative path: {}", imageUrl);
                return imageUrl;
            }
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(absoluteUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = imageDownloadClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                log.warn("Image download failed with status {} for URL: {}", response.statusCode(), absoluteUrl);
                return imageUrl;
            }

            // Read all bytes
            var buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int bytesRead;
            try (InputStream body = response.body()) {
                while ((bytesRead = body.read(chunk)) != -1) {
                    buffer.write(chunk, 0, bytesRead);
                }
            }
            byte[] imageBytes = buffer.toByteArray();

            // Detect MIME type from URL
            String mimeType = detectMimeType(absoluteUrl);
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            log.debug("Image downloaded and encoded: url={}, size={} bytes, mime={}",
                    absoluteUrl, imageBytes.length, mimeType);

            return "data:" + mimeType + ";base64," + base64;
        } catch (Exception e) {
            log.error("Failed to download & encode image: {}", absoluteUrl, e);
            // Fallback: send original URL (may fail, but better than crashing)
            return imageUrl;
        }
    }

    private String detectMimeType(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "image/jpeg"; // safe default
    }
}