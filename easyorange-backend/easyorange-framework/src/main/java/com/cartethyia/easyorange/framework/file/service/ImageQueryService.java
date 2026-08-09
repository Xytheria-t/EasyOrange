package com.cartethyia.easyorange.framework.file.service;

import com.cartethyia.easyorange.framework.config.properties.ImageProcessingProperties;
import com.cartethyia.easyorange.framework.file.service.ImageProcessingService.ImageFormat;
import com.github.benmanes.caffeine.cache.Cache;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ImageQueryService {

    private final FileService fileService;
    private final ImageProcessingService imageProcessingService;
    private final ImageProcessingProperties imageProcessingProperties;
    private final Cache<String, Object> imageProcessCache;

    private static final int[] PRESET_WIDTHS = {150, 300, 600, 1200};

    public ImageQueryService(
            FileService fileService,
            ImageProcessingService imageProcessingService,
            ImageProcessingProperties imageProcessingProperties,
            @Qualifier("imageProcessCache") Cache<String, Object> imageProcessCache) {
        this.fileService = fileService;
        this.imageProcessingService = imageProcessingService;
        this.imageProcessingProperties = imageProcessingProperties;
        this.imageProcessCache = imageProcessCache;
    }

    public record ImageProcessingCacheEntry(File file, String mimeType, String eTag) {}

    public record ImageQueryResult(Resource resource, String mimeType, String eTag, boolean notModified) {}

    public ImageQueryResult getForView(
            String fileId, Integer width, Integer height, ImageFormat format, float quality, String ifNoneMatch)
            throws IOException {
        var fileEntry = resolveFile(fileId);
        if (!imageProcessingService.isImage(fileEntry.mimeType)) {
            return new ImageQueryResult(fileEntry.resource, fileEntry.mimeType, null, false);
        }

        if (width != null || height != null) {
            int tw = width != null ? width : 0;
            int th = height != null ? height : 0;
            if (tw == 0 && th > 0) tw = th;
            else if (th == 0 && tw > 0) th = tw;

            var entry = getCachedOrProcess(fileEntry.path.toFile(), fileId, tw, th, format, quality, ifNoneMatch);
            return toResult(entry);
        }

        if (imageProcessingService.supportsFormat(format)) {
            var dims = imageProcessingService.getDimensions(fileEntry.path);
            var entry = getCachedOrProcess(
                    fileEntry.path.toFile(), fileId, dims.width(), dims.height(), format, quality, ifNoneMatch);
            return toResult(entry);
        }

        return new ImageQueryResult(fileEntry.resource, fileEntry.mimeType, null, false);
    }

    public ImageQueryResult getThumbnail(String fileId, int size, String ifNoneMatch) throws IOException {
        var fileEntry = resolveFile(fileId);
        if (!imageProcessingService.isImage(fileEntry.mimeType)) {
            return new ImageQueryResult(null, null, null, false);
        }
        var entry = getCachedOrProcessForThumbnail(fileEntry.path.toFile(), fileId, size, ifNoneMatch);
        return toResult(entry);
    }

    public ImageQueryResult getResponsive(
            String fileId, Integer width, ImageFormat format, float quality, String ifNoneMatch) throws IOException {
        var fileEntry = resolveFile(fileId);
        if (!imageProcessingService.isImage(fileEntry.mimeType)) {
            return new ImageQueryResult(null, null, null, false);
        }
        // 响应式尺寸按预设宽度取整，宽高一致（方形缩略）
        int targetSize = findClosestPresetWidth(width);
        var entry = getCachedOrProcess(
                fileEntry.path.toFile(), fileId, targetSize, targetSize, format, quality, ifNoneMatch);
        return toResult(entry);
    }

    public void evictCache(String fileId) {
        imageProcessCache.asMap().keySet().removeIf(key -> key.startsWith(fileId + "_"));
        log.info("Evicted image cache for fileId={}", fileId);
    }

    @SuppressWarnings("unchecked")
    private ImageProcessingCacheEntry getFromCache(String cacheKey) {
        var cached = imageProcessCache.getIfPresent(cacheKey);
        return cached != null ? (ImageProcessingCacheEntry) cached : null;
    }

    private void putToCache(String cacheKey, ImageProcessingCacheEntry entry) {
        imageProcessCache.put(cacheKey, entry);
    }

    // ===== Internal =====

    private record FileEntry(Path path, Resource resource, String mimeType) {}

    private FileEntry resolveFile(String fileId) throws IOException {
        var resource = fileService.downloadFile(fileId);
        Path path;
        if (resource instanceof FileSystemResource fsr) {
            path = fsr.getFile().toPath();
        } else {
            path = Files.createTempFile("img_", ".tmp");
            try (var is = resource.getInputStream()) {
                Files.copy(is, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return new FileEntry(path, resource, resolveMimeType(path));
    }

    private static String resolveMimeType(Path path) {
        try {
            var probed = Files.probeContentType(path);
            if (probed != null) return probed;
        } catch (IOException ignored) {
        }
        var name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private static ImageQueryResult toResult(ProcessCacheEntry e) {
        return new ImageQueryResult(
                new FileSystemResource(e.entry().file), e.entry().mimeType, e.entry().eTag, e.notModified);
    }

    private record ProcessCacheEntry(ImageProcessingCacheEntry entry, boolean notModified) {}

    private ProcessCacheEntry getCachedOrProcess(
            File originalFile,
            String fileId,
            int width,
            int height,
            ImageFormat format,
            float quality,
            String ifNoneMatch)
            throws IOException {
        var cacheKey = buildCacheKey(fileId, width, height, format, quality);
        var cached = getFromCache(cacheKey);
        if (cached != null) {
            if (ifNoneMatch != null && ifNoneMatch.equals(cached.eTag())) {
                return new ProcessCacheEntry(cached, true);
            }
            if (cached.file().exists()) {
                return new ProcessCacheEntry(cached, false);
            }
            imageProcessCache.invalidate(cacheKey);
        }

        var processed = imageProcessingService.processImage(originalFile, width, height, format, quality);
        var eTag = computeETag(processed.file());
        var entry = new ImageProcessingCacheEntry(processed.file(), processed.mimeType(), eTag);
        putToCache(cacheKey, entry);
        log.debug("Image processed and cached: key={}", cacheKey);
        return new ProcessCacheEntry(entry, false);
    }

    private ProcessCacheEntry getCachedOrProcessForThumbnail(
            File originalFile, String fileId, int size, String ifNoneMatch) throws IOException {
        var quality = imageProcessingProperties.getThumbnailQuality();
        var cacheKey = buildCacheKey(fileId, size, size, ImageFormat.WEBP, quality);
        var cached = getFromCache(cacheKey);
        if (cached != null) {
            if (ifNoneMatch != null && ifNoneMatch.equals(cached.eTag())) {
                return new ProcessCacheEntry(cached, true);
            }
            if (cached.file().exists()) {
                return new ProcessCacheEntry(cached, false);
            }
            imageProcessCache.invalidate(cacheKey);
        }

        var thumbnail = imageProcessingService.createThumbnail(originalFile, size, quality);
        var eTag = computeETag(thumbnail.file());
        var entry = new ImageProcessingCacheEntry(thumbnail.file(), thumbnail.mimeType(), eTag);
        putToCache(cacheKey, entry);
        log.debug("Thumbnail processed and cached: key={}", cacheKey);
        return new ProcessCacheEntry(entry, false);
    }

    private static String buildCacheKey(String fileId, int width, int height, ImageFormat format, float quality) {
        return "%s_%dx%d_%s_%.0f".formatted(fileId, width, height, format.name(), quality * 100);
    }

    private static String computeETag(File file) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var is = Files.newInputStream(file.toPath())) {
                var buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) digest.update(buf, 0, n);
            }
            return "\"" + HexFormat.of().formatHex(digest.digest()) + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not available", e);
        }
    }

    private static int findClosestPresetWidth(Integer width) {
        if (width == null) return PRESET_WIDTHS[0];
        var closest = PRESET_WIDTHS[0];
        var minDiff = Math.abs(width - closest);
        for (var preset : PRESET_WIDTHS) {
            var diff = Math.abs(width - preset);
            if (diff < minDiff) {
                minDiff = diff;
                closest = preset;
            }
        }
        return closest;
    }
}
