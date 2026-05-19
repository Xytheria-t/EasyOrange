package com.cartethyia.easyorange.framework.util;

import com.cartethyia.easyorange.common.exception.file.FileException;
import com.cartethyia.easyorange.common.exception.file.FileSizeLimitExceededException;
import com.cartethyia.easyorange.common.exception.file.InvalidExtensionException;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class FileUtils {

    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;

    public static final long DEFAULT_MAX_SIZE = 50 * MB;

    public static final int DEFAULT_FILE_NAME_LENGTH = 100;

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public static final Set<String> DEFAULT_ALLOWED_EXTENSION = new LinkedHashSet<>(Set.of(
            "bmp", "gif", "jpg", "jpeg", "png", "webp",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "rar", "zip", "gz", "bz2", "pdf"
    ));

    private static final Map<String, String> MIME_TO_EXTENSION = Map.ofEntries(
            Map.entry("image/bmp", "bmp"),
            Map.entry("image/gif", "gif"),
            Map.entry("image/jpg", "jpg"),
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/png", "png"),
            Map.entry("image/webp", "webp"),
            Map.entry("application/pdf", "pdf"),
            Map.entry("application/msword", "doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
            Map.entry("application/vnd.ms-powerpoint", "ppt"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
            Map.entry("application/x-rar-compressed", "rar"),
            Map.entry("application/zip", "zip"),
            Map.entry("application/x-gzip", "gz"),
            Map.entry("application/x-bzip2", "bz2"),
            Map.entry("video/mp4", "mp4"),
            Map.entry("video/x-msvideo", "avi"),
            Map.entry("video/quicktime", "mov"),
            Map.entry("video/x-ms-wmv", "wmv"),
            Map.entry("video/x-matroska", "mkv"),
            Map.entry("video/webm", "webm"),
            Map.entry("audio/mpeg", "mp3"),
            Map.entry("audio/wav", "wav"),
            Map.entry("audio/x-ms-wma", "wma")
    );

    private static final Map<String, byte[]> FILE_MAGIC_NUMBERS = Map.ofEntries(
            Map.entry("jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
            Map.entry("png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}),
            Map.entry("gif", new byte[]{0x47, 0x49, 0x46, 0x38}),
            Map.entry("bmp", new byte[]{0x42, 0x4D}),
            Map.entry("webp", new byte[]{0x52, 0x49, 0x46, 0x46}),
            Map.entry("pdf", new byte[]{0x25, 0x50, 0x44, 0x46}),
            Map.entry("zip", new byte[]{0x50, 0x4B, 0x03, 0x04}),
            Map.entry("rar", new byte[]{0x52, 0x61, 0x72, 0x21}),
            Map.entry("doc", new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}),
            Map.entry("ppt", new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}),
            Map.entry("xlsx", new byte[]{0x50, 0x4B, 0x03, 0x04}),
            Map.entry("xls", new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0})
    );

    private FileUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String upload(String baseDir, MultipartFile file) throws IOException {
        return upload(baseDir, file, DEFAULT_ALLOWED_EXTENSION, true);
    }

    public static String upload(String baseDir, MultipartFile file, String... allowedExtension) throws IOException {
        Collection<String> extensions = allowedExtension.length > 0
                ? Set.of(allowedExtension)
                : DEFAULT_ALLOWED_EXTENSION;
        return upload(baseDir, file, extensions, true);
    }

    public static String upload(String baseDir, MultipartFile file, Collection<String> allowedExtension, boolean useUuidName) throws IOException {
        int fileNameLength = Objects.requireNonNull(file.getOriginalFilename()).length();
        if (fileNameLength > DEFAULT_FILE_NAME_LENGTH) {
            throw new FileException("文件名长度超出限制：" + DEFAULT_FILE_NAME_LENGTH);
        }
        assertAllowed(file, allowedExtension);
        String fileName = useUuidName ? generateUuidFilename(file) : generateTimestampFilename(file);
        String absPath = getAbsoluteFile(baseDir, fileName).getAbsolutePath();
        file.transferTo(Paths.get(absPath));
        return fileName;
    }

    public static String generateUuidFilename(MultipartFile file) {
        String extension = getExtension(file);
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        return datePath + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }

    public static String generateTimestampFilename(MultipartFile file) {
        String extension = getExtension(file);
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String baseName = getBaseName(file.getOriginalFilename());
        return datePath + "/" + baseName + "_" + System.currentTimeMillis() + "." + extension;
    }

    public static File getAbsoluteFile(String uploadDir, String fileName) throws IOException {
        Path basePath = Paths.get(uploadDir).normalize();
        Path filePath = basePath.resolve(fileName).normalize();
        if (!filePath.startsWith(basePath)) {
            throw new FileException("非法文件路径：" + fileName);
        }
        File desc = filePath.toFile();
        if (!desc.exists()) {
            File parentDir = desc.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                Files.createDirectories(parentDir.toPath());
            }
        }
        return desc;
    }

    public static void assertAllowed(MultipartFile file, Collection<String> allowedExtension) {
        long size = file.getSize();
        if (size > DEFAULT_MAX_SIZE) {
            throw new FileSizeLimitExceededException(DEFAULT_MAX_SIZE, size);
        }
        if (isEmptyCollection(allowedExtension)) {
            return;
        }
        String extension = getExtension(file);
        if (!isAllowedExtension(extension, allowedExtension)) {
            throw new InvalidExtensionException(new java.util.ArrayList<>(allowedExtension), extension, file.getOriginalFilename());
        }
        assertFileMagicNumber(file, extension, allowedExtension);
    }

    private static void assertFileMagicNumber(MultipartFile file, String extension, Collection<String> allowedExtension) {
        byte[] expectedMagic = FILE_MAGIC_NUMBERS.get(extension.toLowerCase());
        if (expectedMagic == null) {
            return;
        }
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[expectedMagic.length];
            int bytesRead = is.read(header);
            if (bytesRead < expectedMagic.length) {
                throw new InvalidExtensionException(
                        new java.util.ArrayList<>(allowedExtension), extension, file.getOriginalFilename());
            }
            for (int i = 0; i < expectedMagic.length; i++) {
                if (header[i] != expectedMagic[i]) {
                    throw new InvalidExtensionException(
                            new java.util.ArrayList<>(allowedExtension), extension, file.getOriginalFilename());
                }
            }
        } catch (IOException e) {
            throw new FileException("文件读取失败：" + file.getOriginalFilename(), e);
        }
    }

    public static boolean isAllowedExtension(String extension, Collection<String> allowedExtension) {
        if (isEmptyCollection(allowedExtension)) {
            return true;
        }
        return allowedExtension.stream()
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
    }

    private static boolean isEmptyCollection(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static String getExtension(MultipartFile file) {
        String extension = null;
        String filename = file.getOriginalFilename();
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf(".") + 1);
        }
        if (extension == null || extension.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType != null) {
                extension = getExtensionFromMimeType(contentType);
            }
        }
        return extension != null ? extension.toLowerCase() : "";
    }

    private static String getExtensionFromMimeType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return MIME_TO_EXTENSION.getOrDefault(contentType.toLowerCase(), "");
    }

    public static String getBaseName(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }

    public static String calculateMd5(MultipartFile file) throws IOException {
        try (var is = file.getInputStream()) {
            return DigestUtils.md5DigestAsHex(is);
        }
    }

    public static String formatFileSize(long size) {
        if (size >= GB) {
            return String.format("%.2f GB", size / (double) GB);
        } else if (size >= MB) {
            return String.format("%.2f MB", size / (double) MB);
        } else if (size >= KB) {
            return String.format("%.2f KB", size / (double) KB);
        } else {
            return size + " B";
        }
    }

    public static boolean deleteFile(String filePath) {
        if (filePath == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean exists(String filePath) {
        if (filePath == null) {
            return false;
        }
        return new File(filePath).exists();
    }

    public static String getFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSeparator == -1) {
            return filePath;
        }
        return filePath.substring(lastSeparator + 1);
    }
}
