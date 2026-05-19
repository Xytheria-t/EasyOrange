package com.cartethyia.easyorange.framework.file.storage;

import com.cartethyia.easyorange.common.exception.file.FileException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Value("${file.upload.path:./upload}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:/api/file/}")
    private String urlPrefix;

    private Path basePath;

    @PostConstruct
    void init() {
        this.basePath = Paths.get(uploadPath).normalize();
    }

    @Override
    public String store(byte[] content, String originalFilename, String contentType) throws IOException {
        String extension = extractExtension(originalFilename, contentType);
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String uuidName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String relativePath = datePath + "/" + uuidName;

        Path fullPath = basePath.resolve(relativePath).normalize();

        if (!fullPath.startsWith(basePath)) {
            throw new FileException("非法文件路径");
        }

        Files.createDirectories(fullPath.getParent());
        Files.write(fullPath, content);

        log.debug("Stored file locally: {} ({} bytes)", relativePath, content.length);
        return relativePath;
    }

    @Override
    public byte[] load(String identifier) throws IOException {
        Path fullPath = basePath.resolve(identifier).normalize();

        if (!fullPath.startsWith(basePath)) {
            throw new FileException("非法文件路径");
        }
        if (!Files.exists(fullPath)) {
            throw new FileException("文件不存在：" + identifier);
        }

        return Files.readAllBytes(fullPath);
    }

    @Override
    public void delete(String identifier) throws IOException {
        Path fullPath = basePath.resolve(identifier).normalize();

        if (!fullPath.startsWith(basePath)) {
            throw new FileException("非法文件路径");
        }

        Files.deleteIfExists(fullPath);
        log.debug("Deleted file: {}", identifier);
    }

    @Override
    public String getUrl(String identifier) {
        return urlPrefix + identifier.replace("\\", "/");
    }

    @Override
    public boolean supportsDirectUrl() {
        return false;
    }

    private String extractExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            if (!ext.isEmpty()) {
                return ext;
            }
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
