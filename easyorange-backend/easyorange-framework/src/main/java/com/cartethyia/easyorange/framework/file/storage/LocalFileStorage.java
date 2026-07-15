package com.cartethyia.easyorange.framework.file.storage;

import com.cartethyia.easyorange.common.exception.file.FileException;
import com.cartethyia.easyorange.framework.config.properties.FileUploadProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileUploadProperties fileUploadProperties;
    private Path basePath;

    public LocalFileStorage(FileUploadProperties fileUploadProperties) {
        this.fileUploadProperties = fileUploadProperties;
    }

    @PostConstruct
    void init() {
        this.basePath = Paths.get(fileUploadProperties.getPath()).normalize();
    }

    @Override
    public String store(byte[] content, String originalFilename, String contentType) throws IOException {
        var extension = extractExtension(originalFilename, contentType);
        var relativePath = "%s/%s.%s".formatted(
                LocalDate.now().format(DATE_PATH_FORMATTER),
                HexFormat.of().formatHex(randomBytes(16)),
                extension);
        var fullPath = securePath(relativePath);
        Files.createDirectories(fullPath.getParent());
        Files.write(fullPath, content);
        log.debug("Stored file locally: {} ({} bytes)", relativePath, content.length);
        return relativePath;
    }

    @Override
    public void delete(String identifier) throws IOException {
        Files.deleteIfExists(securePath(identifier));
        log.debug("Deleted file: {}", identifier);
    }

    @Override
    public Path getPath(String identifier) {
        return securePath(identifier);
    }

    @Override
    public String getUrl(String identifier) {
        return fileUploadProperties.getUrlPrefix() + identifier.replace("\\", "/");
    }

    private Path securePath(String identifier) {
        var resolved = basePath.resolve(identifier).normalize();
        if (!resolved.startsWith(basePath)) {
            throw FileException.of("非法文件路径");
        }
        return resolved;
    }

    private static byte[] randomBytes(int n) {
        var buf = new byte[n];
        ThreadLocalRandom.current().nextBytes(buf);
        return buf;
    }

    private static String extractExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            var ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            if (!ext.isEmpty()) return ext;
        }
        if (contentType != null) {
            return switch (contentType.toLowerCase()) {
                case "image/jpeg", "image/jpg" -> "jpg";
                case "image/png" -> "png";
                case "image/gif" -> "gif";
                case "image/webp" -> "webp";
                case "image/bmp" -> "bmp";
                default -> "bin";
            };
        }
        return "bin";
    }
}
